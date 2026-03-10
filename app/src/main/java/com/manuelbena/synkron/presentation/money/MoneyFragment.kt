package com.manuelbena.synkron.presentation.money


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
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentMoneyBinding
import com.manuelbena.synkron.presentation.models.BudgetPresentationModel
import com.manuelbena.synkron.presentation.models.FilterModel

import com.manuelbena.synkron.presentation.money.adapters.BudgetAdapter
import com.manuelbena.synkron.presentation.note.adapter.FilterAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

// 1. ESTADO DE LA VISTA (Mueve esto a un archivo separado llamado BudgetSummaryState.kt luego)

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {

    // Asegúrate de que tu MoneyViewModel esté en el paquete correcto
    override val viewModel: MoneyViewModel by viewModels()


    private lateinit var budgetAdapter: BudgetAdapter

    // Constantes para identificar cada pestaña (Principio de Magic Numbers = Malo)
    companion object {
        private const val TAB_GENERAL = 1
        private const val TAB_PRESUPUESTOS = 2
        private const val TAB_METAS = 3
        private const val TAB_HISTORIAL = 4
    }

    // Tu adaptador de filtros (Pestañas)
    private lateinit var filterAdapter: FilterAdapter

    override fun inflateView(
        inflater: LayoutInflater, container: ViewGroup?
    ): FragmentMoneyBinding {
        return FragmentMoneyBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        super.setUI()
        setupFilters()
        // Inicializamos el RecyclerView usando el ID exacto de TU XML: rv_budget_categories
        budgetAdapter = BudgetAdapter()
        binding.viewBudgets.rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
            isNestedScrollingEnabled = false // Evita bloqueos de scroll dentro del NestedScrollView
        }
    }

    override fun setListener() {
        super.setListener()

        // Usamos el ID de tu XML: btn_add_budget
        binding.viewBudgets.btnAddBudget.setOnClickListener {
            // Aquí pones la lógica para añadir presupuesto (ej. abrir un BottomSheet)
        }
    }

    override fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // NOTA TÉCNICA: Aquí asumo que en tu MoneyViewModel tienes una variable:
                // val budgetState = MutableStateFlow(BudgetSummaryState())
                // Si no la tienes, créala en tu ViewModel para que esto deje de dar error.
                viewModel.budgetState.collectLatest { state ->
                    renderState(state)
                }
            }
        }
    }

    // El único lugar donde se modifica la UI. Principio de Single Source of Truth.
    private fun renderState(state: BudgetSummaryState) {

        // 1. Actualizar lista de presupuestos
        budgetAdapter.submitList(state.items)

        // 2. Actualizar textos usando los IDs exactos de tu XML
        binding.apply {
            viewBudgets.tvTotalSpentValue.text = state.formattedTotalSpent
            viewBudgets.tvTotalLimitLabel.text = state.formattedTotalLimit
            viewBudgets.tvTotalSpentPercent.text = "${state.totalPercent}%"

            // 3. Actualizar barras de progreso de Google Material (CircularProgressIndicator)
            viewBudgets.lpiTotalProgress.setProgress(state.totalPercent, true)
            viewBudgets.cpiDonutChart.setProgress(state.totalPercent, true)

            // Lógica de UX: Cambiar a rojo si te pasas del presupuesto
            val statusColor = if (state.totalPercent >= 100)
                Color.parseColor("#EF4444") // Rojo
            else
                Color.parseColor("#10B981") // Verde

            viewBudgets.lpiTotalProgress.setIndicatorColor(statusColor)
            viewBudgets.cpiDonutChart.setIndicatorColor(statusColor)
        }
    }

    private fun setupFilters() {
        // 1. Creamos la lista de pestañas
        val tabs = listOf(
            FilterModel(id = TAB_GENERAL, name = "General", isSelected = true),
            FilterModel(id = TAB_PRESUPUESTOS, name = "Presupuestos", isSelected = false),
            FilterModel(id = TAB_METAS, name = "Metas", isSelected = false),
            FilterModel(id = TAB_HISTORIAL, name = "Historial", isSelected = false)
        )

        // 2. Inicializamos el Adapter.
        // El lambda { tabId -> } es el callback que se ejecuta al hacer clic.
        filterAdapter = FilterAdapter(tabs) { selectedTabId ->
            updateContentVisibility(selectedTabId.id)
        }

        // 3. Conectamos el Adapter al RecyclerView de tu XML
        binding.rvFilters.apply {
            // El LayoutManager ya lo tienes en XML, pero por seguridad lo reafirmamos:
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
            itemAnimator = null // Evita parpadeos feos al actualizar el estado de seleccionado
        }

        // 4. Forzamos el estado inicial para que coincida con el XML (General visible, resto oculto)
        updateContentVisibility(TAB_GENERAL)
    }

    // EL CEREBRO DE LA NAVEGACIÓN: Principio de Responsabilidad Única (SRP)
    private fun updateContentVisibility(selectedTabId: Int) {
        binding.apply {
            // OJO TÉCNICO: Usamos .root porque son etiquetas <include>
            viewGeneral.root.isVisible = selectedTabId == TAB_GENERAL
            viewBudgets.root.isVisible = selectedTabId == TAB_PRESUPUESTOS
            viewGoals.root.isVisible = selectedTabId == TAB_METAS
            viewHistory.root.isVisible = selectedTabId == TAB_HISTORIAL
        }
    }
}