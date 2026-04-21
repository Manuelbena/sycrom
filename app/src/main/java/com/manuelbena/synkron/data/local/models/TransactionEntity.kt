package com.manuelbena.synkron.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_table")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val budgetId: Int,       // Identificador del presupuesto al que pertenece
    val amount: Double,      // Cuánto se gastó (ej: 80.0)
    val note: String,        // Ej: "Camiseta Zara"
    val dateMillis: Long,    // Fecha exacta del gasto
    val type: String         // "EXPENSE" (Gasto) o "INCOME" (Ingreso)
)