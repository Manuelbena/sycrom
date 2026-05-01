package com.manuelbena.synkron.presentation.money.models

import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import java.text.NumberFormat
import java.util.Locale

data class BudgetSummaryState(
    val totalLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val items: List<BudgetPresentationModel> = emptyList()
) {
    val totalPercent: Int
        get() = if (totalLimit > 0) ((totalSpent / totalLimit) * 100).toInt() else 0

    val formattedTotalLimit: String
        get() = formatCurrency(totalLimit)

    val formattedTotalSpent: String
        get() = formatCurrency(totalSpent)
}

data class GoalSummaryState(
    val totalTarget: Double = 0.0,
    val totalSaved: Double = 0.0,
    val goals: List<GoalPresentationModel> = emptyList()
) {
    val totalPercent: Int
        get() = if (totalTarget > 0) ((totalSaved / totalTarget) * 100).toInt() else 0

    val formattedTotalTarget: String
        get() = formatCurrency(totalTarget)

    val formattedTotalSaved: String
        get() = formatCurrency(totalSaved)
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(amount)
}
