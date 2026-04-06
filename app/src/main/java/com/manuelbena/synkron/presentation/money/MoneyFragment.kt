package com.manuelbena.synkron.presentation.money

import GoalSummaryState
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentMoneyBinding
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSummaryState
import com.manuelbena.synkron.presentation.money.adapter.GoalsAdapter
import com.manuelbena.synkron.presentation.money.adapters.BudgetAdapter
import com.manuelbena.synkron.presentation.money.dialogs.AddBudgetDialog

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {

    override val viewModel: MoneyViewModel by viewModels()

    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var budgetAdapter: BudgetAdapter

    companion object {
        private const val TAB_GENERAL = 1
        private const val TAB_PRESUPUESTOS = 2
        private const val TAB_METAS = 3
        private const val TAB_HISTORIAL = 4
    }

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentMoneyBinding {
        return FragmentMoneyBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        setupFilters()
        setupGoalsRecyclerView()
        setupBudgetsRecyclerView()
    }

    override fun setListener() {
        super.setListener()
        // Botones flotantes / generales
        binding.viewBudgets.btnAddBudget.setOnClickListener { viewModel.onAddBudgetClicked() }
        binding.viewGoals.btnAddMeta.setOnClickListener { /* Navegar a crear meta */ }
    }

    override fun observe() {
        // Observar estados (UI)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.goalState.collectLatest { renderGoalsState(it) } }
                launch { viewModel.budgetState.collectLatest { renderBudgetState(it) } }
            }
        }

        // Observar Eventos de un solo uso
        viewModel.event.observe(viewLifecycleOwner) { event ->
            handleMoneyEvents(event)
        }
    }

    // --- CONFIGURACIÓN DE LISTAS ---

    private fun setupBudgetsRecyclerView() {
        budgetAdapter = BudgetAdapter(
            onItemClick = { budget -> viewModel.onBudgetClicked(budget) },
            onEditClick = { budget -> viewModel.onEditBudgetClicked(budget) }
        )
        binding.viewBudgets.rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupGoalsRecyclerView() {
        goalsAdapter = GoalsAdapter(
            onAddMoney = { goal, amount -> viewModel.onAddMoneyToGoal(goal, amount) },
            onAddCustomMoney = { goal -> viewModel.onAddCustomMoneyClicked(goal) },
            onDeleteGoal = { goal -> viewModel.onDeleteGoalClicked(goal) }
        )
        binding.viewGoals.rvUpcomingGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalsAdapter
            isNestedScrollingEnabled = false
        }
    }

    // --- RENDERIZADO DE ESTADOS ---

    private fun renderBudgetState(state: BudgetSummaryState) {
        budgetAdapter.submitList(state.items)
        binding.viewBudgets.apply {
            tvTotalSpentValue.text = state.formattedTotalSpent
            tvTotalLimitLabel.text = state.formattedTotalLimit
            tvTotalSpentPercent.text = "${state.totalPercent}%"
            lpiTotalProgress.setProgress(state.totalPercent, true)
            cpiDonutChart.setProgress(state.totalPercent, true)

            val statusColor = if (state.totalPercent >= 100) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
            lpiTotalProgress.setIndicatorColor(statusColor)
            cpiDonutChart.setIndicatorColor(statusColor)
        }
    }

    private fun renderGoalsState(state: GoalSummaryState) {
        goalsAdapter.submitList(state.goals)
        binding.viewGoals.apply {
            tvTotalSavedValue.text = state.formattedTotalSaved
            tvTotalLimitLabel.text = state.formattedTotalTarget
            tvTotalProgressPercent.text = "${state.totalPercent}%"
            lpiTotalMetas.setProgress(state.totalPercent, true)

            val statusColor = if (state.totalPercent >= 50) Color.parseColor("#10B981") else Color.parseColor("#F97316")
            lpiTotalMetas.setIndicatorColor(statusColor)
            tvTotalProgressPercent.setTextColor(statusColor)
        }
    }

    // --- MANEJO DE EVENTOS ---

    private fun handleMoneyEvents(event: MoneyEvents) {
        when (event) {
            is MoneyEvents.ShowAddBudgetDialog -> {
                // AHORA RECIBIMOS CUATRO PARÁMETROS
                val dialog = AddBudgetDialog { emoji, colorHex, title, limit ->
                    viewModel.onSaveNewBudget(emoji, colorHex, title, limit)
                }
                dialog.show(parentFragmentManager, "AddBudgetDialog")
            }
            is MoneyEvents.ShowEditBudgetDialog -> {
                Toast.makeText(requireContext(), "Editar Presupuesto: ${event.budget.name}", Toast.LENGTH_SHORT).show()
            }
            is MoneyEvents.ShowBudgetDetails -> {
                Toast.makeText(requireContext(), "Detalles de: ${event.budget.name}", Toast.LENGTH_SHORT).show()
            }

            is MoneyEvents.ShowAddCustomMoneyDialog -> { /* Dialog añadir dinero a meta */ }
            is MoneyEvents.ShowDeleteGoalConfirmation -> { /* Dialog borrar meta */ }
            is MoneyEvents.ShowError -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
        }
    }

    // --- TABS (Mantenido igual) ---
    private fun setupFilters() {
        val tabLayout = binding.tabLayoutFilters
        tabLayout.addTab(tabLayout.newTab().setText("General"))
        tabLayout.addTab(tabLayout.newTab().setText("Presupuestos"))
        tabLayout.addTab(tabLayout.newTab().setText("Metas"))
        tabLayout.addTab(tabLayout.newTab().setText("Historial"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateContentVisibility((tab?.position ?: 0) + 1)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        updateContentVisibility(TAB_GENERAL)
    }

    private fun updateContentVisibility(selectedTabId: Int) {
        binding.apply {
            viewGeneral.root.isVisible = selectedTabId == TAB_GENERAL
            viewBudgets.root.isVisible = selectedTabId == TAB_PRESUPUESTOS
            viewGoals.root.isVisible = selectedTabId == TAB_METAS
            viewHistory.root.isVisible = selectedTabId == TAB_HISTORIAL
        }
    }
}