package org.bxkr.octodiary.presentation.viewmodel.diary

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.bxkr.octodiary.domain.exception.diary.DiaryException
import org.bxkr.octodiary.domain.exception.diary.UnknownDiaryException
import org.bxkr.octodiary.domain.model.log.LogTemplate
import org.bxkr.octodiary.domain.repository.Logger
import org.bxkr.octodiary.domain.usecase.diary.GetProfileUseCase
import org.bxkr.octodiary.presentation.state.diary.ProfileUiState
import org.bxkr.octodiary.presentation.viewmodel.BaseViewModel
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
class ProfileViewModel(
    private val getProfileUseCase: GetProfileUseCase,
    private val logger: Logger
) : BaseViewModel<ProfileUiState>() {
    override val _uiState: MutableStateFlow<ProfileUiState> = MutableStateFlow(ProfileUiState())

    fun loadProfile() {
        uu { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val result = getProfileUseCase()
                result.fold(
                    onSuccess = { profile ->
                        uu { it.copy(profile = profile) }
                    },
                    onFailure = { exception -> setDiaryError(exception) }
                )
            } catch (throwable: Throwable) {
                setDiaryError(throwable)
            } finally {
                uu { it.copy(isLoading = false) }
            }
        }
    }

    private fun setDiaryError(throwable: Throwable) {
        val diaryException = (throwable as? DiaryException)
            ?: UnknownDiaryException(
                source = "loadProfile() in ProfileViewModel: ${throwable::class.simpleName}: ${throwable.message}"
            )
        if (diaryException is UnknownDiaryException) {
            logger.log(LogTemplate.unknownDiaryException(diaryException))
        }
        uu { it.copy(error = diaryException) }
    }
}