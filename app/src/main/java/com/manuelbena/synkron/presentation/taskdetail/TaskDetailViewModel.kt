package com.manuelbena.synkron.presentation.util // O la ruta que elijas

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para [com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet].
 *
 * Responsabilidades:
 * 1. Recibir la Tarea a mostrar desde [SavedStateHandle].
 * 2. Gestionar el estado de la UI ([TaskDetailContract.TaskDetailState]).
 * 3. Orquestar los casos de uso (Update, Delete).
 * 4. Enviar acciones de un solo uso a la UI (Navigate, Dismiss).
 *
 * @param updateTaskUseCase Caso de uso para actualizar la tarea (ej. check de subtarea).
 * @param deleteTaskUseCase Caso de uso para eliminar la tarea.
 * @param savedStateHandle Handle para recibir la Tarea como argumento de navegación.
 */
@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val updateTaskUseCase: UpdateTaskUseCase,

    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<TaskDetailContract.TaskDetailEvent>() { // No usamos el BaseViewModel genérico aquí

    // --- ESTADO (State) ---
    private val _state = MutableLiveData<TaskDetailContract.TaskDetailState>()
    val state: LiveData<TaskDetailContract.TaskDetailState> = _state

    // --- ACCIÓN (Action) ---
    private val _action = SingleLiveEvent<TaskDetailContract.TaskDetailAction>()
    val action: LiveData<TaskDetailContract.TaskDetailAction> = _action

    /**
     * Tarea actual, la mantenemos internamente para modificarla.
     */
    private lateinit var currentTask: TaskDomain

    init {
        // Obtenemos la tarea pasada como argumento al BottomSheet
        val task = savedStateHandle.get<TaskDomain>("task")
        if (task != null) {
            currentTask = task
            _state.value = TaskDetailContract.TaskDetailState(task)
        } else {
            // Esto no debería pasar si se usa newInstance()
            _action.value = TaskDetailContract.TaskDetailAction.DismissSheet
        }
    }

    /**
     * Manejador central de eventos de la UI.
     */
    fun onEvent(event: TaskDetailContract.TaskDetailEvent) {
        when (event) {
            is TaskDetailContract.TaskDetailEvent.OnSubtaskChecked ->
                handleSubtaskChecked(event.subtask, event.isDone)

            is TaskDetailContract.TaskDetailEvent.OnTaskChecked ->
                handleTaskChecked(event.isDone)

            is TaskDetailContract.TaskDetailEvent.OnDeleteClicked ->
                handleDeleteTask()

            is TaskDetailContract.TaskDetailEvent.OnEditClicked ->
                _action.value = TaskDetailContract.TaskDetailAction.NavigateToEdit(currentTask)

            is TaskDetailContract.TaskDetailEvent.OnShareClicked ->
                _action.value = TaskDetailContract.TaskDetailAction.ShareTask(currentTask)
        }
    }

    /**
     * Actualiza el estado de una subtarea y guarda la tarea completa.
     */
    private fun handleSubtaskChecked(subtask: SubTaskDomain, isDone: Boolean) {
        viewModelScope.launch {
            // Creamos una nueva lista de subtareas con el item actualizado
            val newSubtasks = currentTask.subTasks.map {
                if (it.id == subtask.id) {
                    it.copy(isDone = isDone)
                } else {
                    it
                }
            }
            // Actualizamos la tarea
            currentTask = currentTask.copy(subTasks = newSubtasks)
            updateTaskUseCase(currentTask)

            // Refrescamos la UI con el nuevo estado
            _state.value = TaskDetailContract.TaskDetailState(currentTask)
        }
    }

    /**
     * Actualiza el estado principal (isDone) de la tarea.
     */
    private fun handleTaskChecked(isDone: Boolean) {
        viewModelScope.launch {
            currentTask = currentTask.copy(isDone = isDone)
            updateTaskUseCase(currentTask)

            // Refrescamos la UI
            _state.value = TaskDetailContract.TaskDetailState(currentTask)
        }
    }

    /**
     * Borra la tarea y ordena cerrar el BottomSheet.
     */
    private fun handleDeleteTask() {

    }
}