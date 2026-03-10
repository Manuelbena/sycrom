package com.manuelbena.synkron.presentation.money.BudgetSummary

import com.manuelbena.synkron.presentation.models.BudgetPresentationModel

// presentation/money/BudgetSummaryState.kt

data class BudgetSummaryState(
    val totalLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val items: List<BudgetPresentationModel> = emptyList()
) {
    val totalPercent: Int get() = if (totalLimit > 0) ((totalSpent / totalLimit) * 100).toInt() else 0
    val formattedTotalSpent: String get() = "%.2f €".format(totalSpent)
    val formattedTotalLimit: String get() = "de %.2f €".format(totalLimit)
}