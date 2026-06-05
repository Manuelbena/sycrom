package com.manuelbena.synkron.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_table")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val colorHex: String,
    val deadline: Long? = null
)