package com.manuelbena.synkron.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_contribution_table")
data class GoalContributionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val goalId: Int,
    val amount: Double,
    val dateMillis: Long
)