package com.manuelbena.synkron.presentation.money

import GoalSummaryState
import android.graphics.Color
import android.os.Bundle
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
import com.manuelbena.synkron.presentation.money.BudgetSummary.BudgetSummaryState
import com.manuelbena.synkron.presentation.money.adapter.GoalsAdapter
import com.manuelbena.synkron.presentation.money.adapters.BudgetAdapter
import com.manuelbena.synkron.presentation.money.adapters.CategoryOverviewAdapter
import com.manuelbena.synkron.presentation.money.dialogs.AddBudgetDialog
import com.manuelbena.synkron.presentation.money.dialogs.AddExpenseBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {

    override val viewModel: MoneyViewModel by viewModels()

    private lateinit var goalsAdapter: GoalsAdapter
    private lateinit var budgetAdapter: BudgetAdapter

    private lateinit var categoryOverviewAdapter: CategoryOverviewAdapter

    // ESTADO PARA EL MENÚ FLOTANTE
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
        super.setUI()
        setupFilters()
        setupGoalsRecyclerView()
        setupBudgetsRecyclerView()
        setupOverviewRecyclerView()
    }

    override fun setListener() {
        super.setListener()

        // --- BOTONES FLOTANTES (FAB MENU) ---
        binding.fabMain.setOnClickListener {
            toggleFabMenu()
        }

        binding.fabAddExpense.setOnClickListener {
            toggleFabMenu()
            viewModel.onAddExpenseClicked() // ¡Llama al ViewModel para abrir el gasto!
        }

        binding.fabAddIncome.setOnClickListener {
            toggleFabMenu()
            viewModel.onAddIncomeClicked()
        }

        binding.fabAddGoal.setOnClickListener {
            toggleFabMenu()
            viewModel.onAddGoalClicked()
        }

        binding.fabAddBudget.setOnClickListener {
            toggleFabMenu()
            viewModel.onAddBudgetClicked()
        }

        // --- Botones internos de las vistas vacías ---
        binding.viewBudgets.btnAddBudget.setOnClickListener { viewModel.onAddBudgetClicked() }
        binding.viewGoals.btnAddMeta.setOnClickListener { viewModel.onAddGoalClicked() }
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

    // --- ANIMACIONES DEL FAB MENU ---

    private fun toggleFabMenu() {
        isFabExpanded = !isFabExpanded
        if (isFabExpanded) {
            // Rotar el botón principal a una "X" (45 grados)
            binding.fabMain.animate().rotation(45f).setDuration(200).start()

            // Mostrar los sub-botones con un efecto cascada (retraso según posición)
            showFab(binding.fabAddExpense, 1)
            showFab(binding.fabAddIncome, 2)
            showFab(binding.fabAddGoal, 3)
            showFab(binding.fabAddBudget, 4)
        } else {
            // Devolver a la posición original "+"
            binding.fabMain.animate().rotation(0f).setDuration(200).start()

            // Ocultar los sub-botones
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
            .setStartDelay(0)
            .withEndAction { fab.visibility = View.GONE }
            .start()
    }

    // --- CONFIGURACIÓN DE LISTAS ---
    private fun setupOverviewRecyclerView() {
        categoryOverviewAdapter = CategoryOverviewAdapter()

        // Accedemos al RecyclerView dentro del include 'viewGeneral'
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

    // --- RENDERIZADO DE ESTADOS ---

    private fun renderBudgetState(state: BudgetSummaryState) {
        // Actualiza la pestaña de presupuestos completa
        budgetAdapter.submitList(state.items)

        // ¡NUEVO!: Actualiza la lista cortita de la pestaña "General" (ordenada por gasto)
        // Ordenamos los items para que salgan primero los que tienen más gasto
        // Mostrar TODOS los presupuestos ordenados por el gasto
        val sortedForOverview = state.items.sortedByDescending { it.spent }
        categoryOverviewAdapter.submitList(sortedForOverview)

        // Actualizamos los números grandes de la pestaña General
        binding.viewGeneral.apply {
            tvBalanceValue.text = state.formattedTotalLimit // Usaremos el límite como balance hipotético por ahora
            tvExpenseValue.text = state.formattedTotalSpent

            // Calculamos el % de presupuesto total usado para la tarjeta de abajo
            val totalPercent = state.totalPercent
            // Suponiendo que el TextView de la tarjeta se llama tvBudgetUsedPercent (debes ponerle un ID en tu XML)
            // tvBudgetUsedPercent.text = "$totalPercent%"
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

            // Nuevos eventos del FAB Menu
            is MoneyEvents.ShowAddIncomeDialog -> {
                Toast.makeText(requireContext(), "Abrir Diálogo Ingreso", Toast.LENGTH_SHORT).show()
            }
            is MoneyEvents.ShowAddGoalDialog -> {
                Toast.makeText(requireContext(), "Abrir Diálogo Nueva Meta", Toast.LENGTH_SHORT).show()
            }

            is MoneyEvents.ShowAddCustomMoneyDialog -> { /* Dialog añadir dinero a meta */ }
            is MoneyEvents.ShowDeleteGoalConfirmation -> { /* Dialog borrar meta */ }
            is MoneyEvents.ShowError -> Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            is MoneyEvents.ShowAddExpenseDialog -> {
                val currentBudgets = viewModel.budgetState.value.items
                if (currentBudgets.isEmpty()) {
                    Toast.makeText(requireContext(), "Primero debes crear un presupuesto", Toast.LENGTH_SHORT).show()
                    return
                }

                // ✅ SOLUCIÓN: Recogemos dateMillis y se lo pasamos al ViewModel
                val dialog = AddExpenseBottomSheet(currentBudgets) { budget, amount, note, dateMillis ->
                    viewModel.onSaveExpense(budget, amount, note, dateMillis)
                }
                dialog.show(parentFragmentManager, "AddExpenseBottomSheet")
            }

        }
    }

    // --- TABS ---
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