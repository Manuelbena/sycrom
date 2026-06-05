package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IGoalRepository
import com.manuelbena.synkron.domain.models.GoalDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGoalsUseCase @Inject constructor(
    private val repository: IGoalRepository
) {
    operator fun invoke(): Flow<List<GoalDomain>> = repository.getAllGoals()
}