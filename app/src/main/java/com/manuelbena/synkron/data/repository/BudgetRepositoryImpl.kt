package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.local.models.BudgetDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.domain.interfaces.IBudgetRepository
import com.manuelbena.synkron.domain.models.BudgetDomain
import com.manuelbena.synkron.domain.models.TransactionDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    // IMPLEMENTAMOS LA FUNCIÓN DE TRANSACCIÓN
    override suspend fun insertTransaction(transaction: TransactionDomain) {
        budgetDao.insertTransaction(transaction.toEntity())
    }

    // ASEGÚRATE DE AÑADIR ESTE IMPORT ARRIBA DEL TODO:
    // import kotlinx.coroutines.flow.combine

    override fun getBudgetsWithSpentForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<BudgetDomain>> {
        // 1. Pedimos los presupuestos agrupados
        val budgetsFlow = budgetDao.getBudgetsWithSpentForMonth(startOfMonth, endOfMonth)
        // 2. Pedimos los gastos individuales
        val transactionsFlow = budgetDao.getTransactionsForMonth(startOfMonth, endOfMonth)

        // 3. COMBINAMOS LOS DOS RESULTADOS
        return budgetsFlow.combine(transactionsFlow) { budgetsList, transactionsList ->
            budgetsList.map { budgetSQL ->

                // Filtramos solo los gastos que pertenecen a este presupuesto
                val myTransactions = transactionsList.filter { it.budgetId == budgetSQL.id }

                // Mapeamos al Dominio inyectándole su lista de gastos
                BudgetDomain(
                    id = budgetSQL.id,
                    name = budgetSQL.name,
                    limit = budgetSQL.limitAmount,
                    spent = budgetSQL.spentAmount,
                    emoji = budgetSQL.emoji,
                    colorHex = budgetSQL.colorHex,
                    transactions = myTransactions.map { it.toDomain() } // <-- Le pasamos los gastos al Dominio
                )
            }
        }
    }
}