package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.interfaces.ISuperTaskRepository
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.SuperTaskModel
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val taskRepository: ITaskRepository,
    private val superTaskRepository: ISuperTaskRepository, // Asegúrate de que esto esté en tu módulo DI
) : BaseViewModel<HomeAction>() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    private val _action = SingleLiveEvent<HomeAction>()
    val action: SingleLiveEvent<HomeAction> = _action

    private var tasksJob: Job? = null
    private var superTasksJob: Job? = null // Job separado para SuperTareas

    private val headerDateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES"))

    // Control de Sync Inteligente
    private var lastSyncedYear = -1
    private var lastSyncTime = 0L
    private val SYNC_COOLDOWN = 60 * 1000

    init {
        val today = LocalDate.now()
        loadTasksForDate(today)
        syncYearSmart(today.year)
    }

    fun onDateSelected(date: LocalDate) {
        if (_uiState.value.selectedDate == date) return

        _uiState.update { it.copy(selectedDate = date) }
        loadTasksForDate(date)

        viewModelScope.launch {
            taskRepository.refreshTasksForDate(date)
        }
    }

    fun onRefreshRequested() {
        lastSyncedYear = -1
        syncYearSmart(uiState.value.selectedDate.year, force = true)
    }

    private fun loadTasksForDate(date: LocalDate) {
        // Cancelamos trabajos previos de esta fecha
        tasksJob?.cancel()
        superTasksJob?.cancel()

        _uiState.update { it.copy(selectedDate = date, headerText = formatDateHeader(date)) }

        // 1. Cargar Tareas Normales
        tasksJob = viewModelScope.launch {
            getTaskTodayUseCase(date)
                .distinctUntilChanged()
                .onStart {
                    _uiState.update { it.copy(isLoading = true) }
                    delay(100)
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    showError("Error cargando tareas: ${e.message}")
                }
                .collect { tasks ->
                    _uiState.update { it.copy(tasks = tasks, isLoading = false) }
                }
        }

        // 2. Cargar Super Tareas (NUEVO)
        superTasksJob = viewModelScope.launch {
            superTaskRepository.getSuperTasksForDate(date)
                .catch { e ->
                    // Manejo de error silencioso o log
                    e.printStackTrace()
                }
                .collect { superTasks ->
                    _uiState.update { it.copy(superTasks = superTasks) }
                }
        }
    }

    // --- ACCIONES SUPER TAREAS ---

    fun updateSuperTask(task: SuperTaskModel) {
        viewModelScope.launch {
            try {
                // Asumiendo que tu repositorio tiene saveSuperTask o insertSuperTask
                superTaskRepository.saveSuperTask(task)
                // No hace falta actualizar _uiState manual, el Flow de loadTasksForDate lo hará automático
            } catch (e: Exception) {
                showError("Error al guardar super tarea")
            }
        }
    }

    // --- RESTO DE LÓGICA EXISTENTE ---

    private fun syncYearSmart(year: Int, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || year != lastSyncedYear || (now - lastSyncTime > SYNC_COOLDOWN)) {
            lastSyncedYear = year
            lastSyncTime = now
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                taskRepository.syncYear(year)
                // Recargamos datos tras sync
                loadTasksForDate(uiState.value.selectedDate)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refreshToToday() {
        val today = LocalDate.now()
        if (_uiState.value.selectedDate == today) onRefreshRequested() else loadTasksForDate(today)
    }

    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
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

    fun deleteTaskInstance(task: TaskDomain) {
        executeUseCase(
            useCase = { deleteTaskUseCase.deleteInstance(task) },
            onSuccess = { _action.postValue(HomeAction.ShowErrorSnackbar("Tarea eliminada")) },
            onError = { showError("Error al eliminar") }
        )
    }

    fun deleteTaskSeries(task: TaskDomain) {
        executeUseCase(
            useCase = { deleteTaskUseCase.deleteSeries(task) },
            onSuccess = { _action.postValue(HomeAction.ShowErrorSnackbar("Serie eliminada")) },
            onError = { showError("Error al eliminar serie") }
        )
    }

    private fun showError(message: String) {
        _action.postValue(HomeAction.ShowErrorSnackbar(message))
    }

    private fun formatDateHeader(date: LocalDate): String {
        return date.format(headerDateFormatter).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
    }
}