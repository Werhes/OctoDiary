package org.bxkr.octodiary.ui.screen.diary.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import octodiary4.composeapp.generated.resources.Res
import octodiary4.composeapp.generated.resources.dashboard
import octodiary4.composeapp.generated.resources.free_day
import org.bxkr.octodiary.domain.model.event.Event
import org.bxkr.octodiary.presentation.viewmodel.diary.ProfileViewModel
import org.bxkr.octodiary.presentation.viewmodel.diary.ScheduleViewModel
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFade
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    profileViewModel: ProfileViewModel = koinViewModel(),
    scheduleViewModel: ScheduleViewModel = koinViewModel()
) {
    val profileUiState by profileViewModel.uiState.collectAsState()
    val scheduleUiState by scheduleViewModel.uiState.collectAsState()

    LaunchedEffect(profileUiState.needToLoad) {
        if (profileUiState.needToLoad) profileViewModel.loadProfile()
    }
    LaunchedEffect(scheduleUiState.needToLoad) {
        if (scheduleUiState.needToLoad) scheduleViewModel.loadSchedule()
    }

    Column(Modifier.fillMaxSize()) {
        Text(
            text = profileUiState.profile?.fullName ?: "",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when {
            scheduleUiState.isLoading -> LoadingIndicator(
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            )
            scheduleUiState.loadedEvents != null ->
                TodayLessons(scheduleUiState.loadedEvents.orEmpty())
        }
    }
}

@Composable
private fun TodayLessons(events: List<Event>) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val todaysEvents = events.filter { it.timeStart?.date == today }

    if (todaysEvents.isEmpty()) {
        Text(
            text = stringResource(Res.string.free_day),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(todaysEvents.sortedBy { it.timeStart }, key = { it.eventId }) { event ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.subject?.name ?: event.eventName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    event.timeStart?.let { start ->
                        event.timeEnd?.let { end ->
                            Text(
                                text = "${hhmm(start.hour, start.minute)} - ${hhmm(end.hour, end.minute)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hhmm(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@Composable
fun HomeTitle() {
    Text(stringResource(Res.string.dashboard))
}