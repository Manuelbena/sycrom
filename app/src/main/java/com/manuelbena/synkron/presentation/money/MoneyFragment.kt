package com.manuelbena.synkron.presentation.money

import GoalSummaryState
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentMoneyBinding

import com.manuelbena.synkron.presentation.models.FilterModel
import com.manuelbena.synkron.presentation.money.adapter.GoalsAdapter
import com.manuelbena.synkron.presentation.money.adapters.BudgetAdapter
import com.manuelbena.synkron.presentation.money.adapters.TransactionAdapter

import com.manuelbena.synkron.presentation.note.adapter.FilterAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {

    override val viewModel: MoneyViewModel by viewModels()

    // Adaptadores
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var goalsAdapter: GoalsAdapter

    private lateinit var transactionAdapter: TransactionAdapter

    // Constantes para identificar cada pestaña (Principio de Magic Numbers = Malo)
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

        // 1. Configurar Pestañas
        setupFilters()

        // 2. Configurar RecyclerView de Presupuestos (Usando viewBudgets)
        budgetAdapter = BudgetAdapter()
        binding.viewBudgets.rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
            isNestedScrollingEnabled = false // Evita bloqueos de scroll dentro del NestedScrollView
        }

        // 3. Configurar RecyclerView de Metas (Usando viewGoals)
        goalsAdapter = GoalsAdapter(
            onAddMoney = { goal, amount ->
                // Enviar evento al ViewModel para sumar el dinero
                // Ejemplo: viewModel.addMoneyToGoal(goal.id, amount)
            },
            onAddCustomMoney = { goal ->
                // Aquí deberías mostrar un Dialog o BottomSheet
                // con un teclado numérico para que el usuario escriba el importe.
            },
            onDeleteGoal = { goal ->
                // Aquí puedes mostrar un MaterialAlertDialog de confirmación
                // antes de enviarle el evento al ViewModel para borrarlo.
            }
        )

        binding.viewGoals.rvUpcomingGoals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = goalsAdapter
            isNestedScrollingEnabled = false
        }

        transactionAdapter = TransactionAdapter()
        binding.viewHistory.rvTransactions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            isNestedScrollingEnabled = false // IMPRESCINDIBLE
        }
    }

    override fun setListener() {
        super.setListener()

        // Botón Añadir Presupuesto
        binding.viewBudgets.btnAddBudget.setOnClickListener {
            // Lógica para añadir presupuesto
        }

        // Botón Añadir Meta
        binding.viewGoals.btnAddMeta.setOnClickListener {
            // Lógica para añadir meta
        }
    }

    // UN SOLO MÉTODO OBSERVE PARA GOBERNARLOS A TODOS
    override fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observamos Presupuestos
                launch {
                    viewModel.budgetState.collectLatest { state ->
                        renderBudgetState(state)
                    }
                }

                // Observamos Metas
                launch {
                    viewModel.goalState.collectLatest { state ->
                        renderGoalsState(state)
                    }
                }

                launch {
                    viewModel.historyState.collectLatest { transactions ->
                        transactionAdapter.submitList(transactions)
                    }
                }
            }
        }
    }

    // --- RENDERIZADO DE PRESUPUESTOS ---
    private fun renderBudgetState(state: BudgetSummaryState) {
        budgetAdapter.submitList(state.items)

        binding.viewBudgets.apply {
            tvTotalSpentValue.text = state.formattedTotalSpent
            tvTotalLimitLabel.text = state.formattedTotalLimit
            tvTotalSpentPercent.text = "${state.totalPercent}%"

            lpiTotalProgress.setProgress(state.totalPercent, true)
            cpiDonutChart.setProgress(state.totalPercent, true)

            // Lógica de UX: Cambiar a rojo si te pasas del presupuesto
            val statusColor = if (state.totalPercent >= 100) Color.parseColor("#EF4444") else Color.parseColor("#10B981")

            lpiTotalProgress.setIndicatorColor(statusColor)
            cpiDonutChart.setIndicatorColor(statusColor)
        }
    }

    // --- RENDERIZADO DE METAS ---
    private fun renderGoalsState(state: GoalSummaryState) {
        goalsAdapter.submitList(state.goals)

        binding.viewGoals.apply {
            tvTotalSavedValue.text = state.formattedTotalSaved
            tvTotalLimitLabel.text = state.formattedTotalTarget
            tvTotalProgressPercent.text = "${state.totalPercent}%"

            lpiTotalMetas.setProgress(state.totalPercent, true)

            // Lógica visual: si pasamos del 50%, lo ponemos verde; si no, naranja
            val statusColor = if (state.totalPercent >= 50) Color.parseColor("#10B981") else Color.parseColor("#F97316")

            lpiTotalMetas.setIndicatorColor(statusColor)
            tvTotalProgressPercent.setTextColor(statusColor)
        }
    }

    // --- LÓGICA DE NAVEGACIÓN (PESTAÑAS) ---
    private fun setupFilters() {
        val tabLayout = binding.tabLayoutFilters

        // 1. Añadir las pestañas estáticas
        tabLayout.addTab(tabLayout.newTab().setText("General"))
        tabLayout.addTab(tabLayout.newTab().setText("Presupuestos"))
        tabLayout.addTab(tabLayout.newTab().setText("Metas"))
        tabLayout.addTab(tabLayout.newTab().setText("Historial"))

        // 2. Escuchar los clicks (Sliding Animation nativa)
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Mapeamos la posición del Tab (0, 1, 2, 3) a tus Constantes (1, 2, 3, 4)
                val selectedTabId = (tab?.position ?: 0) + 1
                updateContentVisibility(selectedTabId)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 3. Forzar el estado inicial (Por defecto selecciona el primero - General)
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