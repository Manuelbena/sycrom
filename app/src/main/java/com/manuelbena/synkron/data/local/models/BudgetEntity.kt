package com.manuelbena.synkron.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_table")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val limitAmount: Double,
    val spentAmount: Double = 0.0, // Al crearlo, el gasto inicial es 0
    val emoji: String,
    val colorHex: String
)