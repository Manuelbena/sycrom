package com.manuelbena.synkron.presentation.home

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.GetTasksForDateUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job // <-- IMPORTANTE: Añadir import
import kotlinx.coroutines.flow.catch // <-- IMPORTANTE: Añadir import
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTasksForDate: GetTasksForDateUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : BaseViewModel<HomeEvent>() {

    private val headerDateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    private val selectedDateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))

    // --- MODIFICACIÓN 1: Control de concurrencia y estado ---
    private var loadTasksJob: Job? = null
    private var currentDate: LocalDate = LocalDate.now()
    // --- FIN MODIFICACIÓN 1 ---

    init {
        // Al iniciar, solo enviamos la fecha del saludo.
        // La carga de datos la hará el 'onResume' del Fragment.
        _event.value = HomeEvent.UpdateHeaderText(getTodayHeaderFormatted(currentDate))
    }

    // --- MODIFICACIÓN 2: Nueva función para refrescar ---
    /**
     * Carga las tareas para la fecha guardada (currentDate).
     * Se debe llamar desde onResume() del Fragment.
     */
    fun refreshData() {

        loadTasksForDate(currentDate)
    }



    /**
     * Carga las tareas para una fecha específica desde el repositorio.
     */
    private fun loadTasksForDate(date: LocalDate) {

        loadTasksJob?.cancel()

        currentDate = date
        // --- FIN MODIFICACIÓN 3 ---

        _event.value = HomeEvent.UpdateSelectedDate(getFormattedSelectedDate(date))

        loadTasksJob = viewModelScope.launch {
            getTasksForDate.invoke(date)
                .catch { e ->
                    Log.e(tag, "Error en loadTasksForDate: ${e.message}", e)
                    _event.value = HomeEvent.ShowErrorSnackbar("Error al cargar tareas")
                }
                .collect { taskList ->
                    _event.value = HomeEvent.ListTasksToday(taskList)
                }
        }

    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una TAREA principal.
     */
    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        val updatedTask = task.copy(isDone = isDone)
        executeUseCase(
            useCase = { updateTaskUseCase(updatedTask) },
            onSuccess = {
                _event.value = HomeEvent.UpdateHeaderText(getTodayHeaderFormatted(currentDate))
            },
            onError = { _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la tarea") }
        )
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una SUBTAREA.
     */
    fun onSubtaskCheckedChanged(parentTask: TaskDomain, subtaskToUpdate: SubTaskDomain, isDone: Boolean) {

        val safeId = subtaskToUpdate.id ?: UUID.randomUUID().toString()
        val updatedSubtask = subtaskToUpdate.copy(
            id = safeId,
            isDone = isDone
        )

        val newSubtasks = parentTask.subTasks.map {
            val itId = it.id ?: UUID.randomUUID().toString()
            if (itId == safeId) {
                updatedSubtask
            } else {
                if (it.id == null) it.copy(id = itId) else it
            }
        }

        val allSubtasksDone = newSubtasks.isNotEmpty() && newSubtasks.all { it.isDone }

        val updatedParentTask = parentTask.copy(
            subTasks = newSubtasks,
            isDone = allSubtasksDone
        )

        executeUseCase(
            useCase = { updateTaskUseCase(updatedParentTask) },
            onSuccess = {
                _event.value = HomeEvent.TaskUpdated(updatedParentTask)
            },
            onError = { _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la subtarea") }
        )
    }

    /**
     * Se llama cuando el usuario selecciona una nueva fecha en el calendario.
     */
    fun onDateSelected(date: LocalDate) {
        loadTasksForDate(date)
    }

    /**
     * Maneja las acciones del menú de 3 puntos de una tarea.
     */
    fun onTaskMenuAction(action: TaskAdapter.TaskMenuAction) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (action) {
                    is TaskAdapter.TaskMenuAction.OnDelete -> {
                        // deleteTaskUseCase(action.task)
                    }

                    is TaskAdapter.TaskMenuAction.OnEdit -> {
                        _event.value = HomeEvent.NavigateToEditTask(action.task)
                    }

                    is TaskAdapter.TaskMenuAction.OnShare -> {
                        _event.value = HomeEvent.ShareTask(action.task)
                    }
                }
            } catch (e: Exception) {
                // Manejar errores
            }
        }
    }

    private fun getTodayHeaderFormatted(date: LocalDate): String {
        return date.format(headerDateFormatter)
    }

    private fun getFormattedSelectedDate(date: LocalDate): String {
        return date.format(selectedDateFormatter)
    }
}

