package org.bxkr.octodiary.presentation.state.diary

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import org.bxkr.octodiary.domain.exception.diary.DiaryException
import org.bxkr.octodiary.domain.model.homework.HomeworkEntry

data class HomeworksUiState(
    val isLoading: Boolean = false,
    val error: DiaryException? = null,
    val loadedEntries: List<HomeworkEntry>? = null,
    val loadedEntriesDateRange: LocalDateRange? = null
) {
    val needToLoad get() = loadedEntries == null && !isLoading && error == null
}