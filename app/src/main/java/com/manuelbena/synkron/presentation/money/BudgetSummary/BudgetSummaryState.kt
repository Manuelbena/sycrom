package com.manuelbena.synkron.presentation.money.BudgetSummary

import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import java.util.Locale

data class BudgetSummaryState(
    val totalLimit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val items: List<BudgetPresentationModel> = emptyList()
) {
    val totalPercent: Int get() = if (totalLimit > 0) ((totalSpent / totalLimit) * 100).toInt() else 0
    val formattedTotalSpent: String get() = String.format(Locale.getDefault(), "%.2f €", totalSpent)
    val formattedTotalLimit: String get() = String.format(Locale.getDefault(), "de %.2f €", totalLimit)
}