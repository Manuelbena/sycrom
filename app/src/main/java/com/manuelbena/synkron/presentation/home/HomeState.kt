package com.manuelbena.synkron.presentation.home


import TaskDomain
import java.time.LocalDate

/**
 * Representa el estado de la UI para la pantalla Home.
 * Es inmutable (data class).
 */
data class HomeState(
    val isLoading: Boolean = true,
    val tasks: List<TaskDomain> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val headerText: String = ""
)