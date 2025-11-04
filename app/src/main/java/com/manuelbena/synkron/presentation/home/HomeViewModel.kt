package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.GetTasksForDateUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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

    // --- AÑADIDO ---
    // Dos formateadores, uno para la cabecera y otro para la fecha seleccionada
    private val headerDateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    private val selectedDateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    // --- FIN AÑADIDO ---

    init {
        val today = LocalDate.now()
        // 1. Carga las tareas para HOY
        loadTasksForDate(today)
        // 2. Envía el evento para actualizar el saludo "Hola Manuel, [FECHA DE HOY]"
        _event.value = HomeEvent.UpdateHeaderText(getTodayHeaderFormatted(today))
    }

    /**
     * Carga las tareas para una fecha específica desde el repositorio.
     */
    fun loadTasksForDate(date: LocalDate) {
        executeFlow(
            useCase = { getTasksForDate.invoke(date) },
            onEach = { taskList ->
                _event.value = HomeEvent.ListTasksToday(taskList)
            },
            onError = {
                _event.value = HomeEvent.ShowErrorSnackbar("Error al cargar tareas")
            }
        )
        // Notificamos a la UI que la fecha seleccionada ha cambiado
        _event.value = HomeEvent.UpdateSelectedDate(getFormattedSelectedDate(date))
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una TAREA principal.
     */
    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        val updatedTask = task.copy(isDone = isDone)
        executeUseCase(
            useCase = { updateTaskUseCase(updatedTask) },
            onError = { _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la tarea") }
        )
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una SUBTAREA.
     * Incluye la corrección del NullPointerException.
     */
    fun onSubtaskCheckedChanged(parentTask: TaskDomain, subtaskToUpdate: SubTaskDomain, isDone: Boolean) {

        // 1. "Arregla" el ID si es nulo (datos antiguos)
        val safeId = subtaskToUpdate.id ?: UUID.randomUUID().toString()
        val updatedSubtask = subtaskToUpdate.copy(
            id = safeId,
            isDone = isDone
        )

        // 2. Crea la nueva lista de subtareas "arreglando" IDs nulos
        val newSubtasks = parentTask.subTasks.map {
            // Asigna un ID si es nulo
            val itId = it.id ?: UUID.randomUUID().toString()

            // Compara usando el ID de la subtarea clickada
            if (itId == safeId) {
                updatedSubtask
            } else {
                if (it.id == null) it.copy(id = itId) else it // Guarda el ID arreglado
            }
        }

        // 3. Comprueba si todas las subtareas están hechas
        val allSubtasksDone = newSubtasks.isNotEmpty() && newSubtasks.all { it.isDone }

        // 4. Crea la tarea padre actualizada
        val updatedParentTask = parentTask.copy(
            subTasks = newSubtasks,
            isDone = allSubtasksDone
        )

        // 5. Guarda en BD y notifica al BottomSheet
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

    // --- FUNCIONES DE FORMATO AÑADIDAS ---

    /**
     * Formatea la fecha para la cabecera (Hola Manuel).
     */
    private fun getTodayHeaderFormatted(date: LocalDate): String {
        return date.format(headerDateFormatter)
    }

    /**
     * Formatea la fecha para el título de la tarjeta de tareas (Tareas programadas).
     */
    private fun getFormattedSelectedDate(date: LocalDate): String {
        return date.format(selectedDateFormatter)
    }
    // --- FIN MODIFICACIÓN ---
}

