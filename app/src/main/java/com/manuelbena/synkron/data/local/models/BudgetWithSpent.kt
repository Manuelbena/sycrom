package com.manuelbena.synkron.data.local.models

// Esto NO es una @Entity, es solo un molde para recibir el cruce de datos de SQL
data class BudgetWithSpent(
    val id: Int,
    val name: String,
    val limitAmount: Double,
    val emoji: String,
    val colorHex: String,
    val type: String,
    val spentAmount: Double // Lo calculará SQL sumando las transacciones
)