package com.manuelbena.synkron.presentation.util // O la ruta que elijas

import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain

/**
 * Define el contrato de comunicación entre TaskDetailBottomSheet y TaskDetailViewModel.
 */
interface TaskDetailContract {

    /**
     * El estado de la UI (los datos que se pintan).
     * En este caso, simplemente mostramos la tarea que nos han pasado.
     */
    data class TaskDetailState(
        val task: TaskDomain
    )

    /**
     * Eventos que la UI envía al ViewModel (acciones del usuario).
     */
    sealed class TaskDetailEvent {
        data class OnSubtaskChecked(val subtask: SubTaskDomain, val isDone: Boolean) : TaskDetailEvent()
        data class OnTaskChecked(val isDone: Boolean) : TaskDetailEvent()
        object OnEditClicked : TaskDetailEvent()
        object OnDeleteClicked : TaskDetailEvent()
        object OnShareClicked : TaskDetailEvent()
    }

    /**
     * Acciones de un solo uso que el ViewModel envía a la UI (navegación, etc.).
     */
    sealed class TaskDetailAction {
        data class NavigateToEdit(val task: TaskDomain) : TaskDetailAction()
        data class ShareTask(val task: TaskDomain) : TaskDetailAction()
        object DismissSheet : TaskDetailAction()
    }
}