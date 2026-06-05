package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.domain.models.GoalContributionDomain
import com.manuelbena.synkron.domain.models.GoalDomain
import kotlinx.coroutines.flow.Flow

interface IGoalRepository {
    fun getAllGoals(): Flow<List<GoalDomain>>
    suspend fun insertGoal(goal: GoalDomain)
    suspend fun deleteGoal(goal: GoalDomain)
    suspend fun addMoneyToGoal(goalId: Int, amount: Double, dateMillis: Long)
    fun getContributionsForGoal(goalId: Int): Flow<List<GoalContributionDomain>>
    fun getAllContributions(): Flow<List<GoalContributionDomain>>
}