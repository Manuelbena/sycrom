package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.TransactionDomain
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsBetweenDatesUseCase @Inject constructor(
    private val repository: IBudgetRepository
) {
    operator fun invoke(start: Long, end: Long): Flow<List<TransactionDomain>> {
        return repository.getTransactionsBetweenDates(start, end)
    }
}