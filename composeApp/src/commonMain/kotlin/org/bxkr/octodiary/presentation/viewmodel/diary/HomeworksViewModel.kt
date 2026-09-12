package org.bxkr.octodiary.presentation.viewmodel.diary

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import org.bxkr.octodiary.domain.exception.diary.DiaryException
import org.bxkr.octodiary.domain.exception.diary.UnknownDiaryException
import org.bxkr.octodiary.domain.model.log.LogTemplate
import org.bxkr.octodiary.domain.repository.Logger
import org.bxkr.octodiary.domain.usecase.diary.GetCapabilitiesUseCase
import org.bxkr.octodiary.domain.usecase.diary.GetHomeworkEntriesUseCase
import org.bxkr.octodiary.presentation.state.diary.HomeworksUiState
import org.bxkr.octodiary.presentation.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@KoinViewModel
@OptIn(ExperimentalTime::class)
class HomeworksViewModel(
    private val getHomeworkEntriesUseCase: GetHomeworkEntriesUseCase,
    private val getCapabilitiesUseCase: GetCapabilitiesUseCase,
    private val logger: Logger
) : BaseViewModel<HomeworksUiState>() {
    override val _uiState = MutableStateFlow(HomeworksUiState())

    fun loadHomeworks() {
        uu { it.copy(isLoading = true) }
        viewModelScope.launch {
            val capabilities = getCapabilitiesUseCase()
            val dateRange =
                if (capabilities.isMultipleDatesEventLoadingJustified) getCurrentWeekDateRange()
                else getTodayDateRange()
            val result = getHomeworkEntriesUseCase(dateRange)
            result.fold(
                onSuccess = { entries ->
                    uu {
                        it.copy(
                            loadedEntries = entries,
                            loadedEntriesDateRange = dateRange
                        )
                    }
                },
                onFailure = { exception ->
                    val diaryException = (exception as? DiaryException)
                        ?: UnknownDiaryException(source = "loadHomeworks() in HomeworksViewModel")
                    if (diaryException is UnknownDiaryException) {
                        logger.log(LogTemplate.unknownDiaryException(diaryException))
                    }
                    uu { it.copy(error = diaryException) }
                }
            )
            uu { it.copy(isLoading = false) }
        }
    }

    private fun getTodayDateRange() =
        Clock.System.todayIn(TimeZone.currentSystemDefault()).let { LocalDateRange(it, it) }

    private fun getCurrentWeekDateRange(): LocalDateRange {
        val timeZone = TimeZone.currentSystemDefault()
        var current = Clock.System.todayIn(timeZone)

        while (current.dayOfWeek != DayOfWeek.MONDAY) {
            current = current.minus(1, DateTimeUnit.DAY)
        }

        val monday = current
        val sunday = monday.plus(6, DateTimeUnit.DAY)
        return LocalDateRange(monday, sunday)
    }
}