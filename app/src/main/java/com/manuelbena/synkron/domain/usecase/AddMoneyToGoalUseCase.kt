package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IGoalRepository
import javax.inject.Inject

class AddMoneyToGoalUseCase @Inject constructor(
    private val repository: IGoalRepository
) {
    suspend operator fun invoke(goalId: Int, amount: Double, dateMillis: Long) {
        repository.addMoneyToGoal(goalId, amount, dateMillis)
    }
}