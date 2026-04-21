package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.local.models.BudgetDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.BudgetDomain
import com.manuelbena.synkron.domain.models.TransactionDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : IBudgetRepository {

    override fun getAllBudgets(): Flow<List<BudgetDomain>> {
        return budgetDao.getAllBudgets().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun insertBudget(budget: BudgetDomain) {
        budgetDao.insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(budget: BudgetDomain) {
        budgetDao.deleteBudget(budget.toEntity())
    }

    // --- AQUÍ ESTÁ LA FUNCIÓN ACTUALIZADA (Reemplazado el TODO) ---
    override fun getBudgetsWithSpentForMonth(
        startOfMonth: Long,
        endOfMonth: Long
    ): Flow<List<BudgetDomain>> {
        // Llamamos al DAO que cruza las tablas y lo mapeamos al dominio
        return budgetDao.getBudgetsWithSpentForMonth(startOfMonth, endOfMonth).map { list ->
            list.map { it.toDomain() }
        }
    }

    // IMPLEMENTAMOS LA FUNCIÓN DE TRANSACCIÓN
    override suspend fun insertTransaction(transaction: TransactionDomain) {
        budgetDao.insertTransaction(transaction.toEntity())
    }
}