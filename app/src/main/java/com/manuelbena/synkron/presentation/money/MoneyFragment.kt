package com.manuelbena.synkron.presentation.money

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
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
import com.manuelbena.synkron.presentation.goals.GoalSummaryState
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSummaryState
import com.manuelbena.synkron.presentation.money.adapter.GoalsAdapter
import com.manuelbena.synkron.presentation.money.adapters.BudgetAdapter
import com.manuelbena.synkron.presentation.money.adapters.CategoryOverviewAdapter
import com.manuelbena.synkron.presentation.money.dialogs.AddBudgetDialog
import com.manuelbena.synkron.presentation.money.dialogs.AddExpenseBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {

    override val viewModel: MoneyViewModel by viewModels()

    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))

    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var categoryOverviewAdapter: CategoryOverviewAdapter

    private var isFabExpanded = false

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
        setupFilters()
        setupGoalsRecyclerView()
        setupBudgetsRecyclerView()
        setupOverviewRecyclerView()
    }

    override fun setListener() {
        binding.apply {
            fabMain.setOnClickListener { toggleFabMenu() }
            fabAddExpense.setOnClickListener {
                toggleFabMenu()
                viewModel.onAddExpenseClicked()
            }
            fabAddIncome.setOnClickListener {
                toggleFabMenu()
                viewModel.onAddIncomeClicked()
            }
            fabAddGoal.setOnClickListener {
                toggleFabMenu()
                viewModel.onAddGoalClicked()
            }
            fabAddBudget.setOnClickListener {
                toggleFabMenu()
                viewModel.onAddBudgetClicked()
            }

            viewBudgets.btnAddBudget.setOnClickListener { viewModel.onAddBudgetClicked() }
            viewGoals.btnAddMeta.setOnClickListener { viewModel.onAddGoalClicked() }

            btnNextMonth.setOnClickListener { viewModel.nextMonth() }
            btnPrevMonth.setOnClickListener { viewModel.prevMonth() }
        }
    }

    override fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.goalState.collectLatest { renderGoalsState(it) } }
                launch { viewModel.budgetState.collectLatest { renderBudgetState(it) } }
                launch {
                    viewModel.currentMonth.collectLatest { month ->
                        updateMonthTitle(monthTitleFormatter.format(month))
                    }
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            handleMoneyEvents(event)
        }
    }

    private fun updateMonthTitle(title: String) {
        binding.tvMonthTitle.text = title.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
    }

    private fun toggleFabMenu() {
        isFabExpanded = !isFabExpanded
        val rotation = if (isFabExpanded) 45f else 0f
        binding.fabMain.animate().rotation(rotation).setDuration(200).start()

        if (isFabExpanded) {
            showFab(binding.fabAddExpense, 1)
            showFab(binding.fabAddIncome, 2)
            showFab(binding.fabAddGoal, 3)
            showFab(binding.fabAddBudget, 4)
        } else {
            hideFab(binding.fabAddExpense)
            hideFab(binding.fabAddIncome)
            hideFab(binding.fabAddGoal)
            hideFab(binding.fabAddBudget)
        }
    }

    private fun showFab(fab: View, position: Int) {
        fab.visibility = View.VISIBLE
        fab.alpha = 0f
        fab.translationY = 50f
        fab.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(200)
            .setStartDelay((position * 30).toLong())
            .start()
    }

    private fun hideFab(fab: View) {
        fab.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(200)
            .withEndAction { fab.visibility = View.GONE }
            .start()
    }

    private fun setupOverviewRecyclerView() {
        categoryOverviewAdapter = CategoryOverviewAdapter()
        binding.viewGeneral.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryOverviewAdapter
            isNestedScrollingEnabled = false
        }
    }

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

    private fun renderBudgetState(state: BudgetSummaryState) {
        budgetAdapter.submitList(state.items)
        categoryOverviewAdapter.submitList(state.items.sortedByDescending { it.spent })

        binding.viewGeneral.apply {
            tvBalanceValue.text = state.formattedTotalBalance
            tvExpenseValue.text = state.formattedTotalSpent
            tvIncomeValue.text = state.formattedTotalIncome
        }
    }

    private fun renderGoalsState(state: GoalSummaryState) {
        goalsAdapter.submitList(state.goals)
        binding.viewGoals.apply {
            tvTotalSavedValue.text = state.formattedTotalSaved
            tvTotalLimitLabel.text = state.formattedTotalTarget
            tvTotalProgressPercent.text = String.format(Locale.getDefault(), "%d%%", state.totalPercent)
            lpiTotalMetas.setProgress(state.totalPercent, true)

            val statusColor = if (state.totalPercent >= 50) Color.parseColor("#10B981") else Color.parseColor("#F97316")
            lpiTotalMetas.setIndicatorColor(statusColor)
            tvTotalProgressPercent.setTextColor(statusColor)
        }
    }

    private fun handleMoneyEvents(event: MoneyEvents) {
        when (event) {
            is MoneyEvents.ShowAddBudgetDialog -> {
                AddBudgetDialog { emoji, colorHex, title, limit, type ->
                    viewModel.onSaveNewBudget(emoji, colorHex, title, limit, type)
                }.show(parentFragmentManager, "AddBudgetDialog")
            }
            is MoneyEvents.ShowAddTransactionDialog -> {
                val filteredBudgets = viewModel.budgetState.value.items.filter { it.type == event.type }

                if (filteredBudgets.isEmpty()) {
                    val msg = if (event.type == "EXPENSE") "Crea un presupuesto de gastos" else "Crea un presupuesto de ingresos"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    return
                }

                AddExpenseBottomSheet(filteredBudgets, event.type) { budget, amount, note, dateMillis, type ->
                    viewModel.onSaveExpense(budget, amount, note, dateMillis, type)
                }.show(parentFragmentManager, "AddExpenseBottomSheet")
            }
            is MoneyEvents.ShowError -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            else -> { /* Otros eventos no implementados aún */ }
        }
    }

    private fun setupFilters() {
        val tabLayout = binding.tabLayoutFilters
        tabLayout.apply {
            addTab(newTab().setText("General"))
            addTab(newTab().setText("Presupuestos"))
            addTab(newTab().setText("Metas"))
            addTab(newTab().setText("Historial"))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    updateContentVisibility((tab?.position ?: 0) + 1)
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
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
