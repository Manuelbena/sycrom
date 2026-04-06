package com.manuelbena.synkron.presentation.money

import GoalSummaryState
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.BudgetDomain
import com.manuelbena.synkron.domain.usecase.GetBudgetsUseCase
import com.manuelbena.synkron.domain.usecase.InsertBudgetUseCase
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel
import com.manuelbena.synkron.presentation.models.toPresentation
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSummaryState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MoneyViewModel @Inject constructor(
    // INYECTAMOS NUESTROS CASOS DE USO DE BD
    private val getBudgetsUseCase: GetBudgetsUseCase,
    private val insertBudgetUseCase: InsertBudgetUseCase
) : BaseViewModel<MoneyEvents>() {

    // --- ESTADOS (UI Continuos) ---
    private val _goalState = MutableStateFlow(GoalSummaryState())
    val goalState: StateFlow<GoalSummaryState> = _goalState.asStateFlow()

    private val _budgetState = MutableStateFlow(BudgetSummaryState())
    val budgetState: StateFlow<BudgetSummaryState> = _budgetState.asStateFlow()

    init {

        loadBudgetsFromDatabase() // <-- Ahora cargamos de BD
    }

    // NUEVO: Flujo reactivo con la Base de Datos
    private fun loadBudgetsFromDatabase() {
        executeFlow(
            useCase = { getBudgetsUseCase() },
            onEach = { budgetDomainList ->
                // Mapeamos de Dominio a Presentación
                val presentationItems = budgetDomainList.map { it.toPresentation() }

                // Actualizamos la UI reactivamente
                _budgetState.value = BudgetSummaryState(
                    totalLimit = presentationItems.sumOf { it.limit },
                    totalSpent = presentationItems.sumOf { it.spent },
                    items = presentationItems
                )
            },
            onError = { error ->
                _event.value = MoneyEvents.ShowError("Error al cargar presupuestos: ${error.message}")
            }
        )
    }

    // --- INTENCIONES DE PRESUPUESTOS ---

    // NUEVO: Guardado real en Base de Datos
    fun onSaveNewBudget(emoji: String, colorHex: String, title: String, limit: Double) {
        val newBudget = BudgetDomain(
            name = title,
            limit = limit,
            spent = 0.0, // Al empezar, el gasto es 0
            emoji = emoji,
            colorHex = colorHex
        )

        executeUseCase(
            useCase = { insertBudgetUseCase(newBudget) },
            onSuccess = {
                // Al guardarse, no hace falta recargar la BD manualmente.
                // Room emitirá un nuevo flujo en loadBudgetsFromDatabase() y la pantalla se refrescará sola.
                _event.value = MoneyEvents.ShowError("¡Presupuesto '$title' creado con éxito!")
            },
            onError = {
                _event.value = MoneyEvents.ShowError("No se pudo guardar el presupuesto.")
            }
        )
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
    fun onAddMoneyToGoal(goal: GoalPresentationModel, amount: Double) { /* UseCase add money */ }

    fun onAddCustomMoneyClicked(goal: GoalPresentationModel) {
        _event.value = MoneyEvents.ShowAddCustomMoneyDialog(goal)
    }

    fun onDeleteGoalClicked(goal: GoalPresentationModel) {
        _event.value = MoneyEvents.ShowDeleteGoalConfirmation(goal)
    }
}