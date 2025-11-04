package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
// CAMBIO: Importamos el nuevo Use Case
import com.manuelbena.synkron.domain.usecase.GetTasksForDateUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID // Importamos UUID
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    // CAMBIO: Inyectamos el nuevo Use Case
    private val getTasksForDate: GetTasksForDateUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : BaseViewModel<HomeEvent>() {

    init {
        // CAMBIO: Cargamos las tareas para el día de hoy al iniciar
        loadTasksForDate(LocalDate.now())
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
        // Adicionalmente, notificamos a la UI que la fecha ha cambiado
        _event.value = HomeEvent.UpdateSelectedDate(getFormattedDate(date))
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una TAREA principal.
     */
    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        // Creamos una copia de la tarea con el nuevo estado
        val updatedTask = task.copy(isDone = isDone)

        executeUseCase(
            useCase = { updateTaskUseCase(updatedTask) },
            onError = { _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la tarea") }
        )
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una SUBTAREA
     * (desde el TaskDetailBottomSheet).
     *
     * !! INCLUYE LA CORRECCIÓN DEL NULLPOINTEREXCEPTION !!
     */
    fun onSubtaskCheckedChanged(parentTask: TaskDomain, subtaskToUpdate: SubTaskDomain, isDone: Boolean) {

        // 1. Crear la subtarea actualizada, ASEGURANDO un ID
        // (Solución al NullPointerException de datos antiguos)
        val safeId = subtaskToUpdate.id ?: UUID.randomUUID().toString()
        val updatedSubtask = subtaskToUpdate.copy(
            id = safeId,
            isDone = isDone
        )

        // 2. Crear la nueva lista de subtareas
        val newSubtasks = parentTask.subTasks.map {
            // Comparamos usando el ID seguro.
            // Si 'it.id' es nulo, también le asignamos uno para "arreglarlo"
            val itId = it.id ?: if (it.title == subtaskToUpdate.title) safeId else UUID.randomUUID().toString()

            if (itId == safeId) {
                updatedSubtask
            } else {
                // Si el ID era nulo, lo guardamos ya "arreglado"
                if (it.id == null) it.copy(id = itId) else it
            }
        }

        // 3. Comprobar si *todas* las subtareas (en la nueva lista) están hechas
        val allSubtasksDone = newSubtasks.isNotEmpty() && newSubtasks.all { it.isDone }

        // 4. Crear la tarea padre actualizada
        val updatedParentTask = parentTask.copy(
            subTasks = newSubtasks,
            isDone = allSubtasksDone // La tarea padre se marca como hecha si todas las subtareas lo están
        )

        // 5. Ejecutar el caso de uso (PARA GUARDAR EN BD) y notificar al BottomSheet
        executeUseCase(
            useCase = { updateTaskUseCase(updatedParentTask) }, // <-- ESTO GUARDA EN LA BD
            onSuccess = {
                // Notificamos al BottomSheet que la tarea ha cambiado
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
                        // Llama a tu Caso de Uso para borrar
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

    /**
     * Formatea una fecha (LocalDate) a un String legible (ej: "04 de noviembre de 2025")
     */
    private fun getFormattedDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return date.format(formatter)
    }
}