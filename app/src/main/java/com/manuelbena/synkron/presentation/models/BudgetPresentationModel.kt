package com.manuelbena.synkron.presentation.models

data class BudgetPresentationModel(
    val id: Int,
    val name: String,
    val limit: Double,
    val spent: Double,
    val iconRes: Int,
    val colorHex: String
) {
    val available: Double get() = limit - spent
    val usedPercentage: Int get() = if (limit > 0) ((spent / limit) * 100).toInt() else 0
}