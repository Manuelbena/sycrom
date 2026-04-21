package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.TransactionDomain // Tendrás que crear este modelo igual que la Entidad
import javax.inject.Inject

class InsertTransactionUseCase @Inject constructor(
    private val repository: IBudgetRepository
) {
    suspend operator fun invoke(transaction: TransactionDomain) {
        repository.insertTransaction(transaction)
    }
}