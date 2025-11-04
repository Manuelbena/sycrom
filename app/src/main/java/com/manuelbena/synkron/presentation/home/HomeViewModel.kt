package com.manuelbena.synkron.presentation.home

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.home.adapters.TaskAdapter

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTaskToday: GetTaskTodayUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase // Asegúrate de que esto esté inyectado
) : BaseViewModel<HomeEvent>() {

    init {
        getTaskToday()
    }

    fun getTaskToday() {
        executeFlow(
            useCase = { getTaskToday.invoke() },
            onEach = { taskList ->
                _event.value = HomeEvent.ListTasksToday(taskList)
            },
            onError = {
                _event.value = HomeEvent.ShowErrorSnackbar("Error al cargar tareas")
            }
        )
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una TAREA principal.
     */
    fun onTaskCheckedChanged(task: TaskDomain, isDone: Boolean) {
        // Creamos una copia de la tarea con el nuevo estado
        val updatedTask = task.copy(isDone = isDone)

        // Ejecutamos el caso de uso para actualizar la BD
        executeUseCase(
            useCase = { updateTaskUseCase(updatedTask) },
            onError = { _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la tarea") }
        )
    }

    /**
     * Se llama cuando el usuario marca/desmarca el checkbox de una SUBTAREA
     * (desde el TaskDetailBottomSheet).
     */
    fun onSubtaskCheckedChanged(parentTask: TaskDomain, subtaskToUpdate: SubTaskDomain, isDone: Boolean) {
        // 1. Crear la subtarea actualizada
        val updatedSubtask = subtaskToUpdate.copy(isDone = isDone)

        // 2. Crear la nueva lista de subtareas
        // Esta es la parte clave: mapeamos la lista *antigua* y reemplazamos
        // la subtarea que cambió.
        val newSubtasks = parentTask.subTasks.map {
            if (it.title == subtaskToUpdate.title) { // Asumimos que el título es único por tarea
                updatedSubtask
            } else {
                it
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
                // para que actualice su 'task' interna
                _event.value = HomeEvent.TaskUpdated(updatedParentTask)
            },
            onError = { _event.value = HomeEvent.ShowErrorSnackbar("Error al actualizar la subtarea") }
        )
    }


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

    // El resto de la lógica permanece igual...
    private fun getFormattedDate(): String {
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
        return LocalDate.now().format(formatter)
    }

    fun onDateSelected(date: LocalDate) {
        // Lógica para cargar datos de una nueva fecha
    }

    fun onTaskClicked(task: TaskDomain) {
        // _event.value = HomeEvent.NavigateToTaskDetail(task.id)
    }
}

