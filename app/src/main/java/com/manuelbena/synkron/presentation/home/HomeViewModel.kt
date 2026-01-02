package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.SubTaskDomain
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
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val taskRepository: ITaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _action = SingleLiveEvent<HomeAction>()
    val action: SingleLiveEvent<HomeAction> = _action

    private val _refreshTrigger = MutableSharedFlow<Unit>(
        replay = 1, // Cambiado a 1 para asegurar que se emita si se pierde el evento inicial
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val headerDateFormatter = DateTimeFormatter
        .ofPattern("EEEE, dd 'de' MMMM", Locale("es", "ES"))

    init {
        // Inicializamos la fecha
        _uiState.update { it.copy(selectedDate = LocalDate.now()) }

        viewModelScope.launch {
            // Combinamos los cambios de fecha con el trigger de refresco
            merge(
                _uiState.map { it.selectedDate }.distinctUntilChanged(),
                _refreshTrigger.map { _uiState.value.selectedDate }
            )
                .flatMapLatest { date ->
                    // Llamamos al UseCase directamente
                    getTaskTodayUseCase(date)
                        .onStart {
                            // Activamos carga AL INICIO del flujo
                            _uiState.update { it.copy(isLoading = true) }
                            delay(1000)
                        }
                        .catch { e ->
                            // Manejo de errores
                            _uiState.update { it.copy(isLoading = false) }
                            _action.postValue(HomeAction.ShowErrorSnackbar(e.message ?: "Error al cargar"))
                        }
                        .onEach {
                            // Desactivamos carga en cuanto llega cualquier dato
                            _uiState.update { it.copy(isLoading = false) }
                        }
                }
                .collect { tasks ->
                    // Actualizamos la lista y el texto de la cabecera
                    _uiState.update { currentState ->
                        currentState.copy(
                            tasks = tasks,
                            isLoading = false, // Aseguramos false al recibir datos
                            headerText = currentState.selectedDate.format(headerDateFormatter).replaceFirstChar { char ->
                                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                            }
                        )
                    }
                }
        }
    }

    // Evento del SwipeRefresh
    fun onRefreshRequested() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Sincronización inteligente
                (taskRepository as? com.manuelbena.synkron.data.repository.TaskRepository)?.synchronizeWithGoogle()
                refreshData() // Esto recargará la lista local
                _action.postValue(HomeAction.ShowErrorSnackbar("Sincronizado correctamente"))
            } catch (e: Exception) {
                _action.postValue(HomeAction.ShowErrorSnackbar("Error: ${e.message}"))
            } finally {
                // El isLoading se pondrá en false cuando refreshData() emita los nuevos datos en el init
            }
        }
    }

    fun refreshData() {
        _refreshTrigger.tryEmit(Unit)
    }

    // ... Resto de funciones (onDateSelected, onTaskCheckedChanged, etc.) igual que antes ...
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

    fun onSubTaskChanged(taskId: Int, updatedSubTask: SubTaskDomain) {
        viewModelScope.launch {
            val currentTasks = _uiState.value.tasks
            val parentTask = currentTasks.find { it.id == taskId }

            if (parentTask != null) {
                val newSubTasks = parentTask.subTasks.map {
                    if (it.id == updatedSubTask.id) updatedSubTask else it
                }
                val updatedTask = parentTask.copy(subTasks = newSubTasks)
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