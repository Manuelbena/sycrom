package com.syncro.presentation.home

import androidx.lifecycle.ViewModel
import com.syncro.domain.usecase.GetTasksUseCase
import com.syncro.domain.model.Priority
import com.syncro.domain.model.Subtask
import com.syncro.domain.model.SyncroItem
import com.syncro.presentation.theme.*
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
    val quoteAuthor: String = "Walt Disney",
    val timelineItems: List<SyncroItem> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksUseCase: GetTasksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val items = listOf(
            SyncroItem.Event(
                id = "1",
                title = "Reunión de Diseño",
                description = "Revisar los nuevos mockups de la aplicación con el equipo de producto.",
                startTime = "09:00",
                endTime = "10:30",
                categoryText = "Trabajo",
                categoryColor = Indigo500,
                priority = Priority.HIGH,
                subtasks = listOf(
                    Subtask("Revisar correos", true),
                    Subtask("Preparar presentación", true),
                    Subtask("Coordinar con diseño", false)
                )
            ),
            SyncroItem.Task(
                id = "2",
                title = "Comprar café",
                time = "11:00",
                isCompleted = false
            ),
            SyncroItem.Event(
                id = "3",
                title = "Gimnasio",
                description = "Entrenamiento de pierna y cardio suave.",
                startTime = "18:00",
                endTime = "19:30",
                categoryText = "Personal",
                categoryColor = Emerald500,
                priority = Priority.MEDIUM
            ),
            SyncroItem.Task(
                id = "4",
                title = "Llamar a mamá",
                time = "20:00",
                isCompleted = true
            )
        )
        _uiState.update { it.copy(timelineItems = items) }
    }

    fun onDaySelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun toggleTaskCompletion(taskId: String) {
        _uiState.update { state ->
            state.copy(
                timelineItems = state.timelineItems.map { item ->
                    if (item is SyncroItem.Task && item.id == taskId) {
                        item.copy(isCompleted = !item.isCompleted)
                    } else item
                }
            )
        }
    }

    fun toggleSubtaskCompletion(eventId: String, subtaskTitle: String) {
        _uiState.update { state ->
            state.copy(
                timelineItems = state.timelineItems.map { item ->
                    if (item is SyncroItem.Event && item.id == eventId) {
                        item.copy(
                            subtasks = item.subtasks.map { subtask ->
                                if (subtask.title == subtaskTitle) {
                                    subtask.copy(isCompleted = !subtask.isCompleted)
                                } else subtask
                            }
                        )
                    } else item
                }
            )
        }
    }
}
