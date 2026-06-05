package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.local.models.GoalDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.domain.interfaces.IGoalRepository
import com.manuelbena.synkron.domain.models.GoalContributionDomain
import com.manuelbena.synkron.domain.models.GoalDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : IGoalRepository {

    override fun getAllGoals(): Flow<List<GoalDomain>> {
        return goalDao.getAllGoals().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertGoal(goal: GoalDomain) {
        goalDao.insertGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: GoalDomain) {
        goalDao.deleteGoal(goal.toEntity())
    }

    override suspend fun addMoneyToGoal(goalId: Int, amount: Double, dateMillis: Long) {
        goalDao.addMoneyToGoal(goalId, amount, dateMillis)
    }

    override fun getContributionsForGoal(goalId: Int): Flow<List<GoalContributionDomain>> {
        return goalDao.getContributionsForGoal(goalId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllContributions(): Flow<List<GoalContributionDomain>> {
        return goalDao.getAllContributions().map { list ->
            list.map { it.toDomain() }
        }
    }
}