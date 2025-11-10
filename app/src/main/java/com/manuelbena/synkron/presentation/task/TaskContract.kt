package com.manuelbena.synkron.presentation.task

import com.manuelbena.synkron.domain.models.TaskDomain

/**
 * Define el contrato de comunicación entre TaskFragment (Vista) y TaskViewModel.
 * Sigue el patrón MVI (State, Event, Action).
 */
interface TaskContract {

    /**
     * Define los posibles estados de la UI (lo que se está pintando).
     * El estado es persistente.
     */
    sealed class TaskState {
        object Idle : TaskState() // Estado inicial
        object Loading : TaskState() // Guardando
        data class Success(val message: String) : TaskState() // Se guardó con éxito
        data class Error(val message: String) : TaskState() // Falló al guardar
    }

    /**
     * Define los eventos que la UI envía al ViewModel (acciones del usuario).
     */
    sealed class TaskEvent {
        data class OnSaveTask(val task: TaskDomain) : TaskEvent()
        // Aquí podríamos añadir más, como OnDateChanged, OnTitleChanged, etc.
        // pero por ahora mantenemos la lógica de recolección en el Fragment.
    }

    /**
     * Define acciones de un solo uso (Single-shot) que el ViewModel envía a la UI.
     * (Ej. Navegación, SnackBar).
     */
    sealed class TaskAction {
        object NavigateBack : TaskAction()
        data class ShowErrorSnackbar(val message: String) : TaskAction()
    }
}