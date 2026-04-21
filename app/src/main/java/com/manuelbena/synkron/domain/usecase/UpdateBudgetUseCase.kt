package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.BudgetDomain
import javax.inject.Inject

class UpdateBudgetUseCase @Inject constructor(
    private val repository: IBudgetRepository
) {
    suspend operator fun invoke(budget: BudgetDomain) {
        //repository.updateBudget(budget)
    }
}