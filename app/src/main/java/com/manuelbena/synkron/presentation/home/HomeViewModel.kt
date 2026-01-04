package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.data.repository.TaskRepository
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.DeleteTaskUseCase
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
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
) : BaseViewModel<HomeAction>() { // Heredamos de tu BaseViewModel

    // --- ESTADO (StateFlow para la UI) ---
    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    // --- ACCIONES (Eventos de un solo disparo: Navegación, Mensajes) ---
    // Mantenemos SingleLiveEvent porque es más seguro para Snackbars que el LiveData normal
    private val _action = SingleLiveEvent<HomeAction>()
    val action: SingleLiveEvent<HomeAction> = _action

    // Job para controlar la suscripción al calendario (evita fugas al cambiar de fecha)
    private var tasksJob: Job? = null

    private val headerDateFormatter = DateTimeFormatter
        .ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

    init {
        // Carga inicial
        loadTasksForDate(LocalDate.now())
    }

    // =================================================================
    // 1. CARGA DE DATOS (LECTURA)
    // =================================================================

    fun onDateSelected(date: LocalDate) {
        if (_uiState.value.selectedDate != date) {
            loadTasksForDate(date)
        }
    }

    fun refreshToToday() {
        val today = LocalDate.now()
        if (_uiState.value.selectedDate == today) {
            onRefreshRequested()
        } else {
            loadTasksForDate(today)
        }
    }

    /**
     * Carga las tareas observando la base de datos.
     * NOTA: Aquí usamos un Job manual en lugar de 'executeFlow' porque necesitamos
     * cancelar la suscripción anterior explícitamente cuando el usuario cambia de fecha.
     */
    private fun loadTasksForDate(date: LocalDate) {
        tasksJob?.cancel() // 1. Cancelamos escucha anterior

        // 2. Actualizamos UI con nueva fecha
        _uiState.update {
            it.copy(selectedDate = date, headerText = formatDateHeader(date))
        }

        // 3. Lanzamos nueva escucha
        tasksJob = viewModelScope.launch {
            getTaskTodayUseCase(date)
                .onStart {
                    _uiState.update { it.copy(isLoading = true) }
                    delay(300) // Pequeño delay para suavizar la transición visual
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    showError("Error cargando tareas: ${e.message}")
                }
                .collect { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isLoading = false) }
                }
        }
    }

    fun onRefreshRequested() {
        // Usamos 'executeUseCase' de tu BaseViewModel para la operación de red
        _uiState.update { it.copy(isLoading = true) }

        executeUseCase(
            useCase = {
                (taskRepository as? TaskRepository)?.synchronizeWithGoogle()
            },
            onSuccess = {
                _uiState.update { it.copy(isLoading = false) }
                _action.postValue(HomeAction.ShowErrorSnackbar("Sincronizado correctamente"))
            },
            onError = { e ->
                _uiState.update { it.copy(isLoading = false) }
                showError("Error sincronizando: ${e.message}")
            }
        )
    }

    // =================================================================
    // 2. ACCIONES DE USUARIO (ESCRITURA)
    // =================================================================

    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        // Simplificado con executeUseCase: adiós al viewModelScope.launch manual
        executeUseCase(
            useCase = { updateTaskUseCase(task.copy(isDone = isDone)) },
            onError = { showError("Error al actualizar estado") }
        )
    }

    fun onSubTaskChanged(taskId: Int, updatedSubTask: SubTaskDomain) {
        val parentTask = _uiState.value.tasks.find { it.id == taskId } ?: return

        val newSubTasks = parentTask.subTasks.map {
            if (it.id == updatedSubTask.id) updatedSubTask else it
        }

        executeUseCase(
            useCase = { updateTaskUseCase(parentTask.copy(subTasks = newSubTasks)) },
            onError = { showError("Error al guardar subtarea") }
        )
    }

    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        when (action) {
            is TaskAdapter.TaskMenuAction.OnDelete -> deleteTaskInstance(action.task)
            is TaskAdapter.TaskMenuAction.OnEdit -> _action.postValue(HomeAction.NavigateToEditTask(action.task))
            is TaskAdapter.TaskMenuAction.OnShare -> _action.postValue(HomeAction.ShareTask(action.task))
        }
    }

    // =================================================================
    // 3. BORRADO
    // =================================================================

    fun deleteTaskInstance(task: TaskDomain) {
        executeUseCase(
            useCase = { deleteTaskUseCase.deleteInstance(task) },
            onSuccess = { _action.postValue(HomeAction.ShowErrorSnackbar("Tarea eliminada")) },
            onError = { showError("Error al eliminar: ${it.message}") }
        )
    }

    fun deleteTaskSeries(task: TaskDomain) {
        executeUseCase(
            useCase = { deleteTaskUseCase.deleteSeries(task) },
            onSuccess = { _action.postValue(HomeAction.ShowErrorSnackbar("Serie eliminada")) },
            onError = { showError("Error al eliminar serie") }
        )
    }

    // =================================================================
    // 4. HELPERS
    // =================================================================

    private fun showError(message: String) {
        _action.postValue(HomeAction.ShowErrorSnackbar(message))
    }

    private fun formatDateHeader(date: LocalDate): String {
        return date.format(headerDateFormatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
    }
}