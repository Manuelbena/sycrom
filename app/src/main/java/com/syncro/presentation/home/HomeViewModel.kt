package com.syncro.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syncro.domain.model.SyncroItem
import com.syncro.domain.usecase.GetTasksUseCase
import com.syncro.domain.usecase.SaveTaskUseCase
import com.syncro.domain.usecase.ToggleTaskCompletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "Manuel",
    val selectedDate: LocalDate = LocalDate.now(),
    val quote: String = "La mejor manera de empezar es dejar de hablar y empezar a hacer.",
    val quoteAuthor: String = "Walt Disney",
    val timelineItems: List<SyncroItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Observar tareas del día seleccionado
        _uiState
            .map { it.selectedDate }
            .distinctUntilChanged()
            .flatMapLatest { date ->
                getTasksUseCase(date)
            }
            .onEach { tasks ->
                _uiState.update { it.copy(timelineItems = tasks) }
            }
            .launchIn(viewModelScope)
    }

    fun onDaySelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun saveQuickTask(title: String, description: String, date: LocalDate, time: LocalTime) {
        viewModelScope.launch {
            val timeString = time.format(DateTimeFormatter.ofPattern("HH:mm"))
            saveTaskUseCase(title, description, date, timeString)
        }
    }

    fun toggleTaskCompletion(taskId: String) {
        viewModelScope.launch {
            toggleTaskCompletionUseCase(taskId)
        }
    }

    fun toggleSubtaskCompletion(eventId: String, subtaskTitle: String) {
        // TODO: Implementar cuando los eventos estén en Room
    }
}
