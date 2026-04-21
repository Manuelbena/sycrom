package com.manuelbena.synkron.domain.models

data class TransactionDomain(
    val id: Int = 0,
    val budgetId: Int,
    val amount: Double,
    val note: String,
    val dateMillis: Long,
    val type: String // "EXPENSE" o "INCOME"
)