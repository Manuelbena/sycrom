package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.models.SubTaskDomain // <--- IMPORTANTE
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.DeleteTaskUseCase
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTaskTodayUseCase: GetTaskTodayUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _action = SingleLiveEvent<HomeAction>()
    val action: SingleLiveEvent<HomeAction> = _action

    private val _refreshTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val headerDateFormatter = DateTimeFormatter
        .ofPattern("EEEE, dd 'de' MMMM", Locale("es", "ES"))

    init {
        _uiState.update { it.copy(selectedDate = LocalDate.now()) }

        viewModelScope.launch {
            merge(
                _uiState.map { it.selectedDate }.distinctUntilChanged(),
                _refreshTrigger.map { _uiState.value.selectedDate }
            )
                .flatMapLatest { date ->
                    _uiState.update { it.copy(isLoading = true) }
                    flow {
                        // ELIMINAR O COMENTAR ESTA LÍNEA:
                        // delay(300L) <--- ¡Esto es el culpable del parpadeo lento!

                        getTaskTodayUseCase(date).collect { tasks ->
                            emit(tasks)
                        }
                    }
                }
                .catch { e ->
                    _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al cargar tareas"))
                    _uiState.update { it.copy(isLoading = false) }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = tasks,
                            headerText = it.selectedDate.format(headerDateFormatter).replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                            }
                        )
                    }
                }
        }
    }

    fun refreshData() {
        _refreshTrigger.tryEmit(Unit)
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun refreshToToday() {
        val today = LocalDate.now()
        if (_uiState.value.selectedDate == today) {
            refreshData()
        } else {
            onDateSelected(today)
        }
    }

    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        viewModelScope.launch {
            try {
                updateTaskUseCase(task.copy(isDone = isDone))
            } catch (e: Exception) {
                _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al actualizar"))
            }
        }
    }

    // 🔥 NUEVA FUNCIÓN: Maneja el cambio de una subtarea individual 🔥
    fun onSubTaskChanged(taskId: Int, updatedSubTask: SubTaskDomain) {
        viewModelScope.launch {
            // 1. Buscamos la tarea padre en la lista actual (para no tener que ir a BD a leerla)
            val currentTasks = _uiState.value.tasks
            val parentTask = currentTasks.find { it.id == taskId }

            if (parentTask != null) {
                // 2. Creamos una nueva lista de subtareas reemplazando la modificada
                val newSubTasks = parentTask.subTasks.map {
                    if (it.id == updatedSubTask.id) updatedSubTask else it
                }

                // 3. Copiamos la tarea con la nueva lista
                val updatedTask = parentTask.copy(subTasks = newSubTasks)

                // 4. Guardamos en BD
                try {
                    updateTaskUseCase(updatedTask)
                } catch (e: Exception) {
                    _action.postValue(HomeAction.ShowErrorSnackbar("Error al guardar subtarea"))
                }
            }
        }
    }

    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        viewModelScope.launch {
            when (action) {
                is TaskAdapter.TaskMenuAction.OnDelete -> {
                    try {
                        deleteTaskUseCase(action.task)
                        _action.postValue(HomeAction.ShowErrorSnackbar("Tarea eliminada"))
                    } catch (e: Exception) {
                        _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al eliminar"))
                    }
                }
                is TaskAdapter.TaskMenuAction.OnEdit -> {
                    _action.postValue(HomeAction.NavigateToEditTask(action.task))
                }
                is TaskAdapter.TaskMenuAction.OnShare -> {
                    _action.postValue(HomeAction.ShareTask(action.task))
                }
            }
        }
    }
}