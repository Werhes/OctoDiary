package org.bxkr.octodiary.ui.screen.diary.homeworks

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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import octodiary4.composeapp.generated.resources.Res
import octodiary4.composeapp.generated.resources.free_day
import octodiary4.composeapp.generated.resources.homeworks
import org.bxkr.octodiary.domain.model.homework.HomeworkEntry
import org.bxkr.octodiary.presentation.viewmodel.diary.HomeworksViewModel
import org.bxkr.octodiary.ui.component.AnimatedVisibilityFade
import org.bxkr.octodiary.ui.screen.diary.error.DiaryErrorDescription
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val DateFormat = LocalDate.Format {
    dayOfMonth()
    char('.')
    monthNumber()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeworksScreen(
    viewModel: HomeworksViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.needToLoad) {
        if (uiState.needToLoad) {
            viewModel.loadHomeworks()
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
        AnimatedVisibilityFade(uiState.loadedEntries != null && !uiState.isLoading) {
            HomeworksContent(uiState.loadedEntries.orEmpty())
        }
    }
}

@Composable
private fun HomeworksContent(entries: List<HomeworkEntry>) {
    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.free_day), style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val sorted = rememberSorted(entries)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        items(sorted) { entry ->
            HomeworkCard(entry)
        }
    }
}

@Composable
private fun rememberSorted(entries: List<HomeworkEntry>): List<HomeworkEntry> =
    androidx.compose.runtime.remember(entries) {
        entries.sortedBy { it.deadline }
    }

@Composable
private fun HomeworkCard(entry: HomeworkEntry) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Text(
                    text = entry.subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = entry.deadline.format(DateFormat),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entry.homeworks.forEach { homework ->
                if (!homework.text.isNullOrBlank()) {
                    Text(
                        text = homework.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeworksTitle() {
    Text(stringResource(Res.string.homeworks))
}