package com.manuelbena.synkron.presentation.money

import GoalSummaryState
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.BudgetDomain
import com.manuelbena.synkron.domain.models.TransactionDomain
import com.manuelbena.synkron.domain.usecase.GetBudgetsUseCase
import com.manuelbena.synkron.domain.usecase.InsertBudgetUseCase
import com.manuelbena.synkron.domain.usecase.InsertTransactionUseCase
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import com.manuelbena.synkron.presentation.models.toPresentation
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSummaryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val insertBudgetUseCase: InsertBudgetUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase, // Inyectamos el UseCase de transacciones
) : BaseViewModel<MoneyEvents>() {

    // --- ESTADOS (UI Continuos) ---
    private val _goalState = MutableStateFlow(GoalSummaryState())
    val goalState: StateFlow<GoalSummaryState> = _goalState.asStateFlow()

    private val _budgetState = MutableStateFlow(BudgetSummaryState())
    val budgetState: StateFlow<BudgetSummaryState> = _budgetState.asStateFlow()

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private var loadBudgetsJob: Job? = null

    init {
        loadBudgetsForMonth(_currentMonth.value)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
        loadBudgetsForMonth(_currentMonth.value)
    }

    fun prevMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
        loadBudgetsForMonth(_currentMonth.value)
    }

    // --- CARGA DE DATOS (PRESUPUESTOS) ---
    private fun loadBudgetsForMonth(yearMonth: YearMonth) {
        loadBudgetsJob?.cancel()

        // 1. Calculamos las fechas del 1 al último día del mes seleccionado
        val startOfMonth = yearMonth.atDay(1)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val endOfMonth = yearMonth.atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 2. Pedimos los datos cruzados a Room
        executeFlow(
            useCase = { getBudgetsUseCase(startOfMonth, endOfMonth) },
            onEach = { budgetList ->
                val presentationItems = budgetList.map { it.toPresentation() }

                val expenseBudgets = presentationItems.filter { it.type == "EXPENSE" }
                val incomeBudgets = presentationItems.filter { it.type == "INCOME" }

                _budgetState.value = BudgetSummaryState(
                    totalLimit = expenseBudgets.sumOf { it.limit },
                    totalSpent = expenseBudgets.sumOf { it.spent },
                    totalIncome = incomeBudgets.sumOf { it.spent }, // En INCOME, 'spent' es lo recaudado
                    items = presentationItems
                )
            },
            onError = { error ->
                _event.value = MoneyEvents.ShowError("Error al cargar presupuestos: ${error.message}")
            }
        )
    }

    // --- INTENCIONES DE GUARDADO ---

    fun onSaveNewBudget(emoji: String, colorHex: String, title: String, limit: Double, type: String) {
        val newBudget = BudgetDomain(
            name = title,
            limit = limit,
            spent = 0.0, // Solo usado temporalmente para el mapeo si es necesario
            emoji = emoji,
            colorHex = colorHex,
            type = type
        )

        executeUseCase(
            useCase = { insertBudgetUseCase(newBudget) },
            onSuccess = {
                _event.value = MoneyEvents.ShowError("¡Presupuesto '$title' creado!")
            },
            onError = {
                _event.value = MoneyEvents.ShowError("No se pudo crear el presupuesto.")
            }
        )
    }

    fun onSaveExpense(budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long, type: String) {
        val transaction = TransactionDomain(
            budgetId = budget.id,
            amount = amount,
            note = note,
            dateMillis = dateMillis, // USAMOS LA FECHA QUE ELIGIÓ EL USUARIO
            type = type
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
        _event.value = MoneyEvents.ShowAddTransactionDialog("EXPENSE")
    }

    fun onAddIncomeClicked() {
        _event.value = MoneyEvents.ShowAddTransactionDialog("INCOME")
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