package com.manuelbena.synkron.data.local.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    // Usamos Flow para que la UI se actualice sola si hay cambios en la BD
    @Query("SELECT * FROM budget_table ORDER BY id DESC")
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    @Insert
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("""
        SELECT b.id, b.name, b.limitAmount, b.emoji, b.colorHex, b.type,
               COALESCE(SUM(t.amount), 0.0) AS spentAmount
        FROM budget_table b
        LEFT JOIN transaction_table t 
               ON b.id = t.budgetId 
              AND t.dateMillis >= :startOfMonth 
              AND t.dateMillis <= :endOfMonth
        GROUP BY b.id
    """)
    fun getBudgetsWithSpentForMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<BudgetWithSpent>>

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    // NUEVO: Pedimos TODAS las transacciones del mes ordenadas por fecha (de más nueva a más vieja)
    @Query("SELECT * FROM transaction_table WHERE dateMillis >= :startOfMonth AND dateMillis <= :endOfMonth ORDER BY dateMillis DESC")
    fun getTransactionsForMonth(startOfMonth: Long, endOfMonth: Long): kotlinx.coroutines.flow.Flow<List<TransactionEntity>>
}