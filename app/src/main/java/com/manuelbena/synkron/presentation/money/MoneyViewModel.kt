package com.manuelbena.synkron.presentation.money

import GoalSummaryState
import androidx.lifecycle.ViewModel
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import com.manuelbena.synkron.presentation.models.TransactionPresentationModel
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

    private val _historyState = MutableStateFlow<List<TransactionPresentationModel>>(emptyList())
    val historyState: StateFlow<List<TransactionPresentationModel>> = _historyState.asStateFlow()

    private val _goalState = MutableStateFlow(GoalSummaryState())
    val goalState: StateFlow<GoalSummaryState> = _goalState.asStateFlow()



    init {
        cargarMetasPrueba()
        cargarDatosDePrueba()
        cargarHistorialPrueba()
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

    private fun cargarMetasPrueba() {
        val mockGoals = listOf(
            GoalPresentationModel(
                id = 1,
                title = "Coche nuevo",
                currentAmount = 10000.0,
                targetAmount = 12000.0,
                timeRemaining = "Quedan 2 meses",
                colorHex = "#F97316", // Naranja
                // NUEVO DATO INYECTADO:
                suggestionText = "Para alcanzar tu meta a tiempo deberías ahorrar 1.000 € al mes."
            ),
            GoalPresentationModel(
                id = 2,
                title = "Viaje a Japón",
                currentAmount = 1200.0,
                targetAmount = 3000.0,
                timeRemaining = "Quedan 6 meses",
                colorHex = "#3B82F6", // Azul
                // NUEVO DATO INYECTADO:
                suggestionText = "Te faltan 300 € este mes para mantener el ritmo."
            )
        )

        val totalSaved = mockGoals.sumOf { it.currentAmount }
        val totalTarget = mockGoals.sumOf { it.targetAmount }

        _goalState.value = GoalSummaryState(
            totalSaved = totalSaved,
            totalTarget = totalTarget,
            goals = mockGoals
        )
    }

    // Llama a esto desde tu bloque init {}
    private fun cargarHistorialPrueba() {
        val mockTransactions = listOf(
            TransactionPresentationModel(1, "Mercadona", "18:45 • Alimentación", 45.20, "🛒", false),
            TransactionPresentationModel(2, "Nómina", "08:00 • Ingresos", 1500.00, "💼", true),
            TransactionPresentationModel(3, "Gasolinera Repsol", "14:30 • Transporte", 50.00, "⛽", false),
            TransactionPresentationModel(4, "Netflix", "10:00 • Suscripciones", 12.99, "🍿", false),
            TransactionPresentationModel(5, "Bizum de Ana", "21:15 • Transferencias", 25.00, "💸", true)
        )
        _historyState.value = mockTransactions
    }
}