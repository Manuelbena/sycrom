package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.domain.models.BudgetDomain
import kotlinx.coroutines.flow.Flow

interface IBudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetDomain>>
    suspend fun insertBudget(budget: BudgetDomain)
    suspend fun deleteBudget(budget: BudgetDomain)
}