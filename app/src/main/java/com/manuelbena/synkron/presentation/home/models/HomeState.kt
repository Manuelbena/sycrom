package com.manuelbena.synkron.presentation.home.models



import com.manuelbena.synkron.domain.models.SuperTaskModel
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.WeatherModel
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
    val superTasks: List<SuperTaskModel> = emptyList(),
    val weeklyProductivity: Int = 0,
    val monthlyBalance: Double = 0.0,
    val balanceComparisonPercent: Double = 0.0,
    val weather: WeatherModel? = null
)