package com.manuelbena.synkron.presentation.task


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.TaskDomain

import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la pantalla de Creación/Edición de Tareas.
 *
 * Responsabilidades:
 * 1. Gestionar el estado de la UI ([TaskContract.TaskState]).
 * 2. Recibir eventos de la UI ([TaskContract.TaskEvent]).
 * 3. Enviar acciones de un solo uso ([TaskContract.TaskAction]).
 * 4. Orquestar los casos de uso (ej. [InsertNewTaskUseCase]).
 *
 * @property insertTask Caso de uso para insertar la tarea en el repositorio.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val insertTask: InsertNewTaskUseCase
) : BaseViewModel<TaskContract.TaskEvent>() { // <-- Hereda de BaseViewModel con nuestro Evento

    // --- ESTADO (State) ---
    // Representa el estado persistente de la UI.
    private val _state = MutableLiveData<TaskContract.TaskState>(TaskContract.TaskState.Idle)
    val state: LiveData<TaskContract.TaskState> = _state

    // --- ACCIÓN (Action) ---
    // Representa eventos de un solo uso (navegación, snackbars).
    // Usamos la clase SingleLiveEvent para evitar que se repita en rotaciones.
    private val _action = SingleLiveEvent<TaskContract.TaskAction>()
    val action: LiveData<TaskContract.TaskAction> = _action

    /**
     * Manejador central de eventos provenientes de la UI.
     */
    fun onEvent(event: TaskContract.TaskEvent) {
        when (event) {
            is TaskContract.TaskEvent.OnSaveTask -> onSaveTask(event.task)
        }
    }

    /**
     * Inicia el proceso de guardado de la tarea.
     * Actualiza el estado a Loading, ejecuta el caso de uso y
     * actualiza a Success o Error.
     */
    private fun onSaveTask(task: TaskDomain) {
        viewModelScope.launch {
            _state.value = TaskContract.TaskState.Loading
            try {
                // Llamamos al caso de uso (principio de Clean Architecture)
                insertTask(task)
                _state.value = TaskContract.TaskState.Success("¡Tarea guardada!")
                _action.value = TaskContract.TaskAction.NavigateBack // Enviamos acción para cerrar
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Error desconocido al guardar"
                _state.value = TaskContract.TaskState.Error(errorMsg)
                _action.value = TaskContract.TaskAction.ShowErrorSnackbar(errorMsg)
            }
        }
    }
}