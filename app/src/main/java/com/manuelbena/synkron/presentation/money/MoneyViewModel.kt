package com.manuelbena.synkron.presentation.money

import android.util.Log
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.BudgetDomain
import com.manuelbena.synkron.domain.models.TransactionDomain
import com.manuelbena.synkron.domain.usecase.GetBudgetsUseCase
import com.manuelbena.synkron.domain.usecase.GetTransactionsBetweenDatesUseCase
import com.manuelbena.synkron.domain.usecase.InsertBudgetUseCase
import com.manuelbena.synkron.domain.usecase.InsertTransactionUseCase
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import com.manuelbena.synkron.presentation.models.toPresentation
import com.manuelbena.synkron.presentation.money.models.BudgetSummaryState
import com.manuelbena.synkron.presentation.money.models.GoalSummaryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getTransactionsBetweenDatesUseCase: GetTransactionsBetweenDatesUseCase,
    private val insertBudgetUseCase: InsertBudgetUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase // Inyectamos el UseCase de transacciones
) : BaseViewModel<MoneyEvents>() {

    // --- ESTADOS (UI Continuos) ---
    private val _goalState = MutableStateFlow(GoalSummaryState())
    val goalState: StateFlow<GoalSummaryState> = _goalState.asStateFlow()

    private val _budgetState = MutableStateFlow(BudgetSummaryState())
    val budgetState: StateFlow<BudgetSummaryState> = _budgetState.asStateFlow()

    private val _currentDate = MutableStateFlow(Calendar.getInstance())
    val currentDate: StateFlow<Calendar> = _currentDate.asStateFlow()

    private val _incomeTotal = MutableStateFlow(0.0)
    val incomeTotal: StateFlow<Double> = _incomeTotal.asStateFlow()

    init {
        loadBudgetsForCurrentMonth()
        loadTransactionsForCurrentMonth()
    }

    fun changeMonth(amount: Int) {
        val newDate = _currentDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, amount)
        _currentDate.value = newDate
        loadBudgetsForDate(newDate)
        loadTransactionsForDate(newDate)
    }

    // --- CARGA DE DATOS (PRESUPUESTOS) ---
    private fun loadBudgetsForCurrentMonth() {
        loadBudgetsForDate(_currentDate.value)
    }

    private fun loadBudgetsForDate(date: Calendar) {
        val calendar = date.clone() as Calendar

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        executeFlow(
            useCase = { getBudgetsUseCase(startOfMonth, endOfMonth) },
            onEach = { budgetList ->
                Log.d("DEBUG_BUDGET", "--- CARGA DE PRESUPUESTOS ---")
                budgetList.forEach { 
                    Log.d("DEBUG_BUDGET", "ID: ${it.id}, Nombre: ${it.name}, Tipo: ${it.type}")
                }
                
                val presentationItems = budgetList.map { it.toPresentation() }

                _budgetState.value = BudgetSummaryState(
                    totalLimit = presentationItems.sumOf { it.limit },
                    totalSpent = presentationItems.sumOf { it.spent },
                    items = presentationItems
                )
            },
            onError = { error ->
                Log.e("DEBUG_BUDGET", "❌ Error al cargar: ${error.message}")
                _event.value = MoneyEvents.ShowError("Error al cargar presupuestos: ${error.message}")
            }
        )
    }

    // --- INTENCIONES DE GUARDADO ---

    fun onSaveNewBudget(emoji: String, colorHex: String, title: String, limit: Double, type: String) {
        Log.d("DEBUG_BUDGET", "--- INICIO GUARDADO PRESUPUESTO ---")
        Log.d("DEBUG_BUDGET", "Título: $title, Tipo: $type, Límite: $limit")
        
        val newBudget = BudgetDomain(
            name = title,
            limit = limit,
            spent = 0.0,
            emoji = emoji,
            colorHex = colorHex,
            type = type
        )

        executeUseCase(
            useCase = { insertBudgetUseCase(newBudget) },
            onSuccess = {
                Log.d("DEBUG_BUDGET", "✅ Éxito al insertar en BD")
                _event.value = MoneyEvents.ShowError("¡Categoría '$title' creada!")
                loadBudgetsForCurrentMonth()
                loadTransactionsForCurrentMonth()
            },
            onError = {
                Log.e("DEBUG_BUDGET", "❌ Error al insertar: ${it.message}")
                _event.value = MoneyEvents.ShowError("No se pudo crear la categoría.")
            }
        )
    }

    private fun loadTransactionsForCurrentMonth() {
        loadTransactionsForDate(_currentDate.value)
    }

    private fun loadTransactionsForDate(date: Calendar) {
        val calendar = date.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfMonth = calendar.timeInMillis

        executeFlow(
            useCase = { getTransactionsBetweenDatesUseCase(startOfMonth, endOfMonth) },
            onEach = { transactions ->
                val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                _incomeTotal.value = totalIncome
                
                // También actualizamos el balance en budgetState si es necesario
                val totalSpent = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                _budgetState.value = _budgetState.value.copy(
                    totalSpent = totalSpent,
                    totalLimit = _budgetState.value.totalLimit // Mantener el limite
                )
            }
        )
    }

    fun onSaveIncome(budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long) {
        val transaction = TransactionDomain(
            budgetId = budget.id, // Usamos el ID del presupuesto seleccionado
            amount = amount,
            note = note,
            dateMillis = dateMillis,
            type = "INCOME"
        )

        executeUseCase(
            useCase = { insertTransactionUseCase(transaction) },
            onSuccess = {
                _event.value = MoneyEvents.ShowError("Ingreso guardado correctamente")
            },
            onError = {
                _event.value = MoneyEvents.ShowError("Error al guardar el ingreso")
            }
        )
    }

    fun onSaveExpense(budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long) {
        val transaction = TransactionDomain(
            budgetId = budget.id,
            amount = amount,
            note = note,
            dateMillis = dateMillis, // USAMOS LA FECHA QUE ELIGIÓ EL USUARIO
            type = "EXPENSE"
        )

        executeUseCase(
            useCase = { insertTransactionUseCase(transaction) },
            onSuccess = {
                _event.value = MoneyEvents.ShowError("Gasto guardado correctamente")
            },
            onError = {
                _event.value = MoneyEvents.ShowError("Error al guardar el gasto")
            }
        )
    }

    // --- EVENTOS DEL MENÚ FLOTANTE Y NAVEGACIÓN ---

    fun onAddExpenseClicked() {
        _event.value = MoneyEvents.ShowAddExpenseDialog
    }

    fun onAddIncomeClicked() {
        _event.value = MoneyEvents.ShowAddIncomeDialog
    }

    fun onAddGoalClicked() {
        _event.value = MoneyEvents.ShowAddGoalDialog
    }

    fun onAddBudgetClicked() {
        _event.value = MoneyEvents.ShowAddBudgetDialog
    }

    fun onEditBudgetClicked(budget: BudgetPresentationModel) {
        _event.value = MoneyEvents.ShowEditBudgetDialog(budget)
    }

    fun onBudgetClicked(budget: BudgetPresentationModel) {
        _event.value = MoneyEvents.ShowBudgetDetails(budget)
    }

    // --- INTENCIONES DE METAS ---
    fun onAddMoneyToGoal(goal: GoalPresentationModel, amount: Double) { /* Próximamente */ }

    fun onAddCustomMoneyClicked(goal: GoalPresentationModel) {
        _event.value = MoneyEvents.ShowAddCustomMoneyDialog(goal)
    }

    fun onDeleteGoalClicked(goal: GoalPresentationModel) {
        _event.value = MoneyEvents.ShowDeleteGoalConfirmation(goal)
    }
}