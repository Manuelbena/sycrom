package com.manuelbena.synkron.presentation.money

import androidx.lifecycle.ViewModel
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject

// ESTADO CENTRALIZADO
data class BudgetSummaryState(
    val totalLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val items: List<BudgetPresentationModel> = emptyList()
) {
    val totalPercent: Int get() = if (totalLimit > 0) ((totalSpent / totalLimit) * 100).toInt() else 0
    val formattedTotalSpent: String get() = String.format(Locale.getDefault(), "%.2f €", totalSpent)
    val formattedTotalLimit: String get() = String.format(Locale.getDefault(), "de %.2f €", totalLimit)
}

@HiltViewModel
class MoneyViewModel @Inject constructor() : ViewModel() {

    private val _budgetState = MutableStateFlow(BudgetSummaryState())
    val budgetState: StateFlow<BudgetSummaryState> = _budgetState.asStateFlow()

    init {
        cargarDatosDePrueba()
    }

    private fun cargarDatosDePrueba() {
        val mockBudgets = listOf(
            BudgetPresentationModel(
                id = 1,
                name = "Alimentación",
                limit = 600.0,
                spent = 380.0,
                iconRes = com.manuelbena.synkron.R.drawable.ic_health,
                colorHex = "#EA580C"
            ),
            BudgetPresentationModel(
                id = 2,
                name = "Transporte",
                limit = 200.0,
                spent = 500.0,
                iconRes = com.manuelbena.synkron.R.drawable.ic_location,
                colorHex = "#0EA5E9"
            )
        )

        val totalLimit = mockBudgets.sumOf { it.limit }
        val totalSpent = mockBudgets.sumOf { it.spent }

        _budgetState.value = BudgetSummaryState(
            totalLimit = totalLimit,
            totalSpent = totalSpent,
            items = mockBudgets
        )
    }
}