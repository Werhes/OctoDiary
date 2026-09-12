package org.bxkr.octodiary.ui.screen.diary.marks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import octodiary4.composeapp.generated.resources.Res
import octodiary4.composeapp.generated.resources.marks
import octodiary4.composeapp.generated.resources.no_marks
import org.bxkr.octodiary.domain.model.event.Event
import org.bxkr.octodiary.domain.model.mark.Mark
import org.bxkr.octodiary.domain.model.mark.MarkComponent
import org.bxkr.octodiary.presentation.viewmodel.diary.ScheduleViewModel
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFade
import org.bxkr.octodiary.ui.screen.diary.error.DiaryErrorDescription
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MarksScreen(
    viewModel: ScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.needToLoad) {
        if (uiState.needToLoad) viewModel.loadSchedule()
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
            MarksContent(uiState.loadedEvents.orEmpty())
        }
    }
}

@Composable
private fun MarksContent(events: List<Event>) {
    val marks = rememberMarks(events)
    if (marks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.no_marks), style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val bySubject = rememberGrouped(marks)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        bySubject.forEach { (subjectName, subjectMarks) ->
            item(key = subjectName) { SubjectHeader(subjectName, subjectMarks) }
            items(subjectMarks, key = { it.hashCode() }) { mark ->
                MarkCard(mark)
            }
        }
    }
}

@Composable
private fun rememberMarks(events: List<Event>): List<Mark> =
    androidx.compose.runtime.remember(events) { events.flatMap { it.marks } }

@Composable
private fun rememberGrouped(marks: List<Mark>): List<Pair<String, List<Mark>>> =
    androidx.compose.runtime.remember(marks) {
        marks.groupBy { it.subject.name }
            .map { (name, list) -> name to list.sortedByDescending { integerValueOf(it) } }
    }

@Composable
private fun SubjectHeader(subjectName: String, subjectMarks: List<Mark>) {
    val average = subjectMarks.mapNotNull { averageOf(it) }
        .let { values -> if (values.isEmpty()) null else values.average() }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        Text(
            text = subjectName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        average?.let {
            Text(
                text = formatAverage(it),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MarkCard(mark: Mark) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                mark.workType?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                mark.setBy?.fullName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = markLabel(mark),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun markLabel(mark: Mark): String {
    val counted = mark.components.filterIsInstance<MarkComponent.IntegerMark>()
        .filter { it.isCounted }
    if (counted.isNotEmpty()) {
        val avg = counted.map { it.value }.average()
        return formatAverage(avg)
    }
    return mark.components.firstOrNull()?.let { componentLabel(it) } ?: "?"
}

private fun formatAverage(value: Double): String {
    val int = value.toInt()
    return if (value == int.toDouble()) {
        int.toString()
    } else {
        val tenth = ((value * 10).toInt()) % 10
        "$int.$tenth"
    }
}

private fun componentLabel(component: MarkComponent): String = when (component) {
    is MarkComponent.IntegerMark -> component.value.toString()
    is MarkComponent.BoolMark -> component.label
    is MarkComponent.StringMark -> component.label
}

private fun integerValueOf(mark: Mark): Int =
    mark.components.filterIsInstance<MarkComponent.IntegerMark>()
        .filter { it.isCounted }
        .firstOrNull()?.value ?: 0

private fun averageOf(mark: Mark): Double? {
    val integers = mark.components.filterIsInstance<MarkComponent.IntegerMark>()
        .filter { it.isCounted }
    return if (integers.isNotEmpty()) integers.map { it.value }.average() else null
}

@Composable
fun MarksTitle() {
    Text(stringResource(Res.string.marks))
}