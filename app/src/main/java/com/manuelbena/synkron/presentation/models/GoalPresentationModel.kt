package com.manuelbena.synkron.presentation.models

import java.util.Locale


data class GoalPresentationModel(
    val id: Int,
    val title: String,
    val currentAmount: Double,
    val targetAmount: Double,
    val timeRemaining: String,
    val colorHex: String,
    val suggestionText: String // <--- NUEVA PROPIEDAD REQUERIDA
) {
    // La lógica de cálculo pertenece al modelo, no a la vista
    val progressPercent: Int get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt() else 0

    val formattedProgress: String get() = String.format(Locale.getDefault(), "%,.0f € / %,.0f €", currentAmount, targetAmount)
    val formattedPercent: String get() = "${progressPercent}%"
}