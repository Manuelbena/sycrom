package com.syncro.presentation.home

import androidx.lifecycle.ViewModel
import com.syncro.domain.usecase.GetTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "Manuel",
    val selectedDate: LocalDate = LocalDate.now(),
    val quote: String = "La mejor manera de empezar es dejar de hablar y empezar a hacer.",
    val quoteAuthor: String = "Walt Disney"
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    fun onDaySelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }
}
