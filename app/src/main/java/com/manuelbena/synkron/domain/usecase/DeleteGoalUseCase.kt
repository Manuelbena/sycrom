package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IGoalRepository
import com.manuelbena.synkron.domain.models.GoalDomain
import javax.inject.Inject

class DeleteGoalUseCase @Inject constructor(
    private val repository: IGoalRepository
) {
    suspend operator fun invoke(goal: GoalDomain) = repository.deleteGoal(goal)
}