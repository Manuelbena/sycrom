package com.manuelbena.synkron.presentation.home

import com.manuelbena.synkron.domain.models.TaskDomain


// Eventos de un solo uso que el ViewModel puede enviar a la UI

// Clase sellada para las Acciones (Eventos de un solo uso)
sealed class HomeAction {
    data class NavigateToEditTask(val task: TaskDomain) : HomeAction()
    data class ShowErrorSnackbar(val message: String) : HomeAction()
    data class ShareTask(val task: TaskDomain) : HomeAction()
}