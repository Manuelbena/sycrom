package com.manuelbena.synkron.data.local.models

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal_table")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goal_table WHERE id = :id")
    suspend fun getGoalById(id: Int): GoalEntity?

    // Contributions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: GoalContributionEntity)

    @Query("SELECT * FROM goal_contribution_table WHERE goalId = :goalId")
    fun getContributionsForGoal(goalId: Int): Flow<List<GoalContributionEntity>>
    
    @Query("SELECT * FROM goal_contribution_table")
    fun getAllContributions(): Flow<List<GoalContributionEntity>>

    @Transaction
    suspend fun addMoneyToGoal(goalId: Int, amount: Double, dateMillis: Long) {
        val goal = getGoalById(goalId)
        if (goal != null) {
            val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
            updateGoal(updatedGoal)
            insertContribution(GoalContributionEntity(goalId = goalId, amount = amount, dateMillis = dateMillis))
        }
    }
}