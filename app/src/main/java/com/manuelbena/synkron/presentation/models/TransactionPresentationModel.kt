package com.manuelbena.synkron.presentation.models

import java.util.Locale

data class TransactionPresentationModel(
    val id: Int,
    val title: String,
    val subtitle: String, // Ej: "14:30 • Alimentación"
    val amount: Double,
    val emoji: String,    // Usaremos Emojis reales como "🍔", "🛒", "💰"
    val isIncome: Boolean // true = Verde (+), false = Oscuro (-)
) {
    val formattedAmount: String get() {
        val sign = if (isIncome) "+" else "-"
        return String.format(Locale.getDefault(), "%s %,.2f €", sign, amount)
    }
}