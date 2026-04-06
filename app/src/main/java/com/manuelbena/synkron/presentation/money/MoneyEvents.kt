package com.manuelbena.synkron.presentation.money

import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.GoalPresentationModel

sealed class MoneyEvents {
    // --- Eventos de Ahorros / Metas ---
    data class ShowAddCustomMoneyDialog(val goal: GoalPresentationModel) : MoneyEvents()
    data class ShowDeleteGoalConfirmation(val goal: GoalPresentationModel) : MoneyEvents()

    // --- NUEVO: Eventos de Presupuestos ---
    object ShowAddBudgetDialog : MoneyEvents()
    data class ShowEditBudgetDialog(val budget: BudgetPresentationModel) : MoneyEvents()
    data class ShowBudgetDetails(val budget: BudgetPresentationModel) : MoneyEvents()

    // --- Generales ---
    data class ShowError(val message: String) : MoneyEvents()
}