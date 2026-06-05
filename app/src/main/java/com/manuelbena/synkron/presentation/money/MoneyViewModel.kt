package com.manuelbena.synkron.presentation.money

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.*
import com.manuelbena.synkron.domain.usecase.*
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import com.manuelbena.synkron.presentation.models.toPresentation
import com.manuelbena.synkron.presentation.money.models.BudgetSummaryState
import com.manuelbena.synkron.presentation.money.models.GoalSummaryState
import com.manuelbena.synkron.presentation.money.MoneyEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor(
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val getTransactionsBetweenDatesUseCase: GetTransactionsBetweenDatesUseCase,
    private val insertBudgetUseCase: InsertBudgetUseCase,
    private val insertTransactionUseCase: InsertTransactionUseCase,
    private val getGoalsUseCase: GetGoalsUseCase,
    private val insertGoalUseCase: InsertGoalUseCase,
    private val addMoneyToGoalUseCase: AddMoneyToGoalUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val getGoalContributionsUseCase: GetGoalContributionsUseCase
) : BaseViewModel<MoneyEvents>() {

    private val _goalState = MutableStateFlow(GoalSummaryState())
    val goalState: StateFlow<GoalSummaryState> = _goalState.asStateFlow()

    private val _budgetState = MutableStateFlow(BudgetSummaryState())
    val budgetState: StateFlow<BudgetSummaryState> = _budgetState.asStateFlow()

    private val _currentDate = MutableStateFlow<Calendar>(Calendar.getInstance())
    val currentDate: StateFlow<Calendar> = _currentDate.asStateFlow()

    private val _incomeTotal = MutableStateFlow(0.0)
    val incomeTotal: StateFlow<Double> = _incomeTotal.asStateFlow()

    init {
        loadBudgetsForCurrentMonth()
        loadTransactionsForCurrentMonth()
        loadGoals()
    }

    fun changeMonth(offset: Int) {
        val newDate = _currentDate.value.clone() as Calendar
        newDate.add(Calendar.MONTH, offset)
        _currentDate.value = newDate
        loadBudgetsForDate(newDate)
        loadTransactionsForDate(newDate)
    }

    fun loadBudgetsForCurrentMonth() {
        loadBudgetsForDate(_currentDate.value)
    }

    fun loadBudgetsForDate(calendar: Calendar) {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)

        executeFlow(
            useCase = { getBudgetsUseCase(start.timeInMillis, end.timeInMillis) },
            onEach = { domainList ->
                val presentationItems = domainList.map { it.toPresentation() }
                val totalSpent = domainList.filter { it.type == "GASTO" || it.type == "EXPENSE" }.sumOf { it.spent }
                val totalLimit = domainList.filter { it.type == "GASTO" || it.type == "EXPENSE" }.sumOf { it.limit }

                _budgetState.value = BudgetSummaryState(
                    items = presentationItems,
                    totalSpent = totalSpent,
                    totalLimit = totalLimit
                )
            }
        )
    }

    fun onSaveNewBudget(emoji: String, color: String, title: String, limit: Double, type: String) {
        viewModelScope.launch {
            val newBudget = BudgetDomain(
                name = title,
                limit = limit,
                spent = 0.0,
                emoji = emoji,
                colorHex = color,
                type = type
            )
            insertBudgetUseCase(newBudget)
        }
    }

    fun loadTransactionsForCurrentMonth() {
        loadTransactionsForDate(_currentDate.value)
    }

    fun loadTransactionsForDate(calendar: Calendar) {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)

        executeFlow(
            useCase = { getTransactionsBetweenDatesUseCase(start.timeInMillis, end.timeInMillis) },
            onEach = { transactions ->
                val totalIncome = transactions.filter { it.type == "INCOME" || it.type == "INGRESO" }.sumOf { it.amount }
                _incomeTotal.value = totalIncome
            }
        )
    }

    fun onSaveIncome(budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long) {
        viewModelScope.launch {
            insertTransactionUseCase(
                TransactionDomain(
                    budgetId = budget.id,
                    amount = amount,
                    note = note,
                    dateMillis = dateMillis,
                    type = "INCOME"
                )
            )
        }
    }

    fun onSaveExpense(budget: BudgetPresentationModel, amount: Double, note: String, dateMillis: Long) {
        viewModelScope.launch {
            insertTransactionUseCase(
                TransactionDomain(
                    budgetId = budget.id,
                    amount = amount,
                    note = note,
                    dateMillis = dateMillis,
                    type = "EXPENSE"
                )
            )
        }
    }

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

    fun onExportExcelClicked() {
        viewModelScope.launch {
            val year = _currentDate.value.get(Calendar.YEAR)
            val start = Calendar.getInstance().apply {
                set(year, Calendar.JANUARY, 1, 0, 0, 0)
            }.timeInMillis
            val end = Calendar.getInstance().apply {
                set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            }.timeInMillis

            val transactions = getTransactionsBetweenDatesUseCase(start, end).first()
            val budgets = _budgetState.value.items

            val csv = generateDetailedMatrixCsvReport(year, transactions, budgets)
            _event.value = MoneyEvents.ExportExcel(csv)
        }
    }

    private fun generateDetailedMatrixCsvReport(year: Int, transactions: List<TransactionDomain>, budgets: List<BudgetPresentationModel>): String {
        val sb = StringBuilder()
        sb.append('\ufeff') // UTF-8 BOM

        val months = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre", "Total")
        
        sb.append("Actualizado: $year;")
        months.forEach { sb.append("$it;") }
        sb.append("\n")

        val incomeBudgets = budgets.filter { it.type.equals("INCOME", true) || it.type.equals("INGRESO", true) }
        val expenseBudgets = budgets.filter { it.type.equals("EXPENSE", true) || it.type.equals("GASTO", true) }

        fun formatCsvAmount(amount: Double): String {
            return String.format(Locale.getDefault(), "%.2f €", amount).replace(".", ",")
        }

        fun appendSection(budgetsInSection: List<BudgetPresentationModel>, labelTotal: String): DoubleArray {
            val sectionTotals = DoubleArray(12) { 0.0 }
            
            budgetsInSection.forEach { budget ->
                sb.append("${budget.name.uppercase()};")
                var rowTotal = 0.0
                val monthTotals = DoubleArray(12) { 0.0 }
                
                for (m in 0..11) {
                    val amount = transactions.filter { tx ->
                        val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                        cal.get(Calendar.MONTH) == m && tx.budgetId == budget.id
                    }.sumOf { it.amount }
                    monthTotals[m] = amount
                    rowTotal += amount
                    sectionTotals[m] += amount
                }
                
                monthTotals.forEach { sb.append("${formatCsvAmount(it)};") }
                sb.append("${formatCsvAmount(rowTotal)}\n")

                val categoryTransactions = transactions.filter { it.budgetId == budget.id }.sortedBy { it.dateMillis }
                categoryTransactions.forEach { tx ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.dateMillis }
                    val m = cal.get(Calendar.MONTH)
                    val day = cal.get(Calendar.DAY_OF_MONTH)
                    
                    sb.append("  ($day) ${tx.note};")
                    for (i in 0..11) {
                        if (i == m) sb.append("${formatCsvAmount(tx.amount)};")
                        else sb.append(";")
                    }
                    sb.append(";\n")
                }
            }
            
            sb.append("$labelTotal;")
            var grandTotal = 0.0
            sectionTotals.forEach {
                grandTotal += it
                sb.append("${formatCsvAmount(it)};")
            }
            sb.append("${formatCsvAmount(grandTotal)}\n")
            return sectionTotals
        }

        sb.append("INGRESOS;;\n")
        val incomeTotals = appendSection(incomeBudgets, "TOTAL INGRESOS")
        sb.append(";\n")

        sb.append("GASTOS;;\n")
        val expenseTotals = appendSection(expenseBudgets, "TOTAL GASTOS")
        sb.append(";\n")

        sb.append("AHORRO (Ingresos - Gastos);")
        var totalAhorroYear = 0.0
        for (m in 0..11) {
            val saving = incomeTotals[m] - expenseTotals[m]
            totalAhorroYear += saving
            sb.append("${formatCsvAmount(saving)};")
        }
        sb.append("${formatCsvAmount(totalAhorroYear)}\n")

        return sb.toString()
    }

    fun onEditBudgetClicked(budget: BudgetPresentationModel) {
        _event.value = MoneyEvents.ShowEditBudgetDialog(budget)
    }

    fun onBudgetClicked(budget: BudgetPresentationModel) {
        _event.value = MoneyEvents.ShowBudgetDetails(budget)
    }

    // --- INTENCIONES DE METAS ---

    private fun loadGoals() {
        viewModelScope.launch {
            getGoalsUseCase().collectLatest { goals ->
                getGoalContributionsUseCase.getAll().collectLatest { contributions ->
                    val year = _currentDate.value.get(Calendar.YEAR)
                    val month = _currentDate.value.get(Calendar.MONTH)

                    val goalItems = goals.map { goal ->
                        val thisMonthSaved = contributions.filter {
                            val cal = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                            it.goalId == goal.id && cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
                        }.sumOf { it.amount }

                        val remaining = goal.targetAmount - goal.currentAmount
                        val suggestion = if (remaining <= 0) "¡Meta alcanzada!" 
                                        else "Este mes has aportado ${String.format(Locale.getDefault(), "%.2f", thisMonthSaved)} €"

                        GoalPresentationModel(
                            id = goal.id,
                            title = goal.title,
                            currentAmount = goal.currentAmount,
                            targetAmount = goal.targetAmount,
                            timeRemaining = "Sin límite",
                            colorHex = goal.colorHex,
                            suggestionText = suggestion
                        )
                    }

                    val totalSaved = goals.sumOf { it.currentAmount }
                    val totalTarget = goals.sumOf { it.targetAmount }

                    _goalState.value = GoalSummaryState(
                        goals = goalItems,
                        totalSaved = totalSaved,
                        totalTarget = totalTarget
                    )
                }
            }
        }
    }

    fun onSaveNewGoal(title: String, target: Double, color: String) {
        viewModelScope.launch {
            insertGoalUseCase(GoalDomain(title = title, targetAmount = target, colorHex = color))
        }
    }

    fun onAddMoneyToGoal(goal: GoalPresentationModel, amount: Double) {
        viewModelScope.launch {
            addMoneyToGoalUseCase(goal.id, amount, System.currentTimeMillis())
        }
    }

    fun onAddCustomMoneyClicked(goal: GoalPresentationModel) {
        _event.value = MoneyEvents.ShowAddCustomMoneyDialog(goal)
    }

    fun onDeleteGoalClicked(goal: GoalPresentationModel) {
        viewModelScope.launch {
            deleteGoalUseCase(GoalDomain(id = goal.id, title = goal.title, targetAmount = goal.targetAmount, colorHex = goal.colorHex))
        }
    }
}
