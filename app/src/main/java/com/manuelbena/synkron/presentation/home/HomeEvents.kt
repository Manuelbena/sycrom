package com.manuelbena.synkron.presentation.home

import com.manuelbena.synkron.domain.models.TaskDomain


// Eventos de un solo uso que el ViewModel puede enviar a la UI
sealed interface HomeEvent {
    data class NavigateToTaskDetail(val taskId: String) : HomeEvent
    data class ShowErrorSnackbar(val message: String) : HomeEvent
    data class  ListTasksToday(val list : List <TaskDomain>) : HomeEvent
    data class NavigateToEditTask(val task: TaskDomain) : HomeEvent
    data class ShareTask(val task: TaskDomain) : HomeEvent

    data class TaskUpdated(val task: TaskDomain) : HomeEvent
}