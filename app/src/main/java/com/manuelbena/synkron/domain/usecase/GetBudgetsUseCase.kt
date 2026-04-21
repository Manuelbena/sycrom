package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.BudgetDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBudgetsUseCase @Inject constructor(
    private val repository: IBudgetRepository
) {
    // AHORA RECIBE LOS PARÁMETROS DE FECHA
    operator fun invoke(startOfMonth: Long, endOfMonth: Long): Flow<List<BudgetDomain>> {
        return repository.getBudgetsWithSpentForMonth(startOfMonth, endOfMonth)
    }
}