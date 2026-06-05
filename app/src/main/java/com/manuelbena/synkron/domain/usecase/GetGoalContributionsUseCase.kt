package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IGoalRepository
import com.manuelbena.synkron.domain.models.GoalContributionDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoalContributionsUseCase @Inject constructor(
    private val repository: IGoalRepository
) {
    fun getForGoal(goalId: Int): Flow<List<GoalContributionDomain>> = repository.getContributionsForGoal(goalId)
    fun getAll(): Flow<List<GoalContributionDomain>> = repository.getAllContributions()
}