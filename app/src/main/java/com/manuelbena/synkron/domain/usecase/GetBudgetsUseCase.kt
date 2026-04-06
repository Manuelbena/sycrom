package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.BudgetDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetsUseCase @Inject constructor(
    private val repository: IBudgetRepository
) {
    operator fun invoke(): Flow<List<BudgetDomain>> = repository.getAllBudgets()
}