package com.manuelbena.synkron.presentation.home



import com.manuelbena.synkron.domain.models.SuperTaskModel
import com.manuelbena.synkron.domain.models.TaskDomain
import java.time.LocalDate

/**
 * Representa el estado de la UI para la pantalla Home.
 * Es inmutable (data class).
 */
data class HomeState(
    val isLoading: Boolean = true,
    val tasks: List<TaskDomain> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val headerText: String = "",
    val superTasks: List<SuperTaskModel> = emptyList() // <--- NUEVO
)