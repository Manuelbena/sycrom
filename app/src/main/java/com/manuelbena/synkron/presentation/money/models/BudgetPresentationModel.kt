package com.manuelbena.synkron.presentation.models

import com.manuelbena.synkron.domain.models.BudgetDomain

data class BudgetPresentationModel(
    val id: Int = 0,
    val name: String,
    val limit: Double,
    val spent: Double,
    val emoji: String,
    val colorHex: String,
    val transactions: List<TransactionPresentationModel> = emptyList() // Usará el que ya tienes creado
) {
    val available: Double get() = limit - spent
    val usedPercentage: Int get() = if (limit > 0) ((spent / limit) * 100).toInt() else 0
}

// Mapper de Dominio a Presentación
fun BudgetDomain.toPresentation() = BudgetPresentationModel(
    id = id,
    name = name,
    limit = limit,
    spent = spent,
    emoji = emoji,
    colorHex = colorHex,
    transactions = transactions.map { it.toPresentation() } // Mapea usando tu función
)