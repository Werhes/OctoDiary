package org.bxkr.octodiary.ui.screen.diary.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import org.bxkr.octodiary.domain.model.event.Event
import org.bxkr.octodiary.presentation.viewmodel.diary.ScheduleViewModel
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFade
import org.bxkr.octodiary.ui.screen.diary.error.DiaryErrorDescription
import octodiary4.composeapp.generated.resources.Res
import octodiary4.composeapp.generated.resources.diary
import octodiary4.composeapp.generated.resources.free_day
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val DayFormat = LocalDate.Format {
    dayOfMonth()
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.needToLoad) {
        if (uiState.needToLoad) {
            viewModel.loadSchedule()
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibilityFade(uiState.isLoading) { LoadingIndicator(Modifier.align(Alignment.Center)) }
        AnimatedVisibilityFade(uiState.error != null && !uiState.isLoading) {
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                uiState.error?.let { DiaryErrorDescription(it) }
            }
        }
        AnimatedVisibilityFade(uiState.loadedEvents != null && !uiState.isLoading) {
            ScheduleContent(uiState.loadedEvents.orEmpty())
        }
    }
}

@Composable
private fun ScheduleContent(events: List<Event>) {
    val days = remember(events) { events.mapNotNull { it.timeStart?.date }.distinct().sorted() }

    if (days.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.free_day), style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    var selectedDay by remember(events) { mutableStateOf(days.first()) }
    if (selectedDay !in days) selectedDay = days.first()

    val eventsByDay = remember(events) { events.groupBy { it.timeStart?.date } }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { day ->
                FilterChip(
                    selected = day == selectedDay,
                    onClick = { selectedDay = day },
                    label = { Text(formatDay(day)) }
                )
            }
        }
        LessonList(eventsByDay[selectedDay].orEmpty())
    }
}

@Composable
private fun LessonList(events: List<Event>) {
    val sorted = remember(events) { events.sortedBy { it.timeStart } }
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(sorted, key = { it.eventId }) { event ->
            LessonCard(event)
        }
    }
}

@Composable
private fun LessonCard(event: Event) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = event.subject?.name ?: event.eventName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val timeText = remember(event) {
                val start = event.timeStart
                val end = event.timeEnd
                if (start != null && end != null) "${formatTime(start.hour, start.minute)} - ${formatTime(end.hour, end.minute)}" else null
            }
            timeText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!event.employee?.fullName.isNullOrBlank()) {
                Text(
                    text = event.employee!!.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            event.location?.name?.let { locationName ->
                Text(
                    text = locationName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDay(date: LocalDate): String = date.format(DayFormat)

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
fun ScheduleTitle() {
    Text(stringResource(Res.string.diary))
}