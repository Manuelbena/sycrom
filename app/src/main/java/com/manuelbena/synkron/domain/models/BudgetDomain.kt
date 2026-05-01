package com.manuelbena.synkron.domain.models

data class BudgetDomain(
    val id: Int = 0,
    val name: String,
    val limit: Double,
    val spent: Double,
    val emoji: String,
    val colorHex: String,
    val transactions: List<TransactionDomain> = emptyList() // <-- AÑADE ESTA LÍNEA
)