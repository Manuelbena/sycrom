package com.manuelbena.synkron.presentation.money

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.manuelbena.synkron.R // Importante para los iconos
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentMoneyBinding

import com.manuelbena.synkron.presentation.models.CategorySendModel
import com.manuelbena.synkron.presentation.models.FilterModel
import com.manuelbena.synkron.presentation.money.adapter.CategoryAdapter
import com.manuelbena.synkron.presentation.note.adapter.FilterAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MoneyFragment : BaseFragment<FragmentMoneyBinding, MoneyViewModel>() {

    override val viewModel: MoneyViewModel by viewModels()

    // Adaptador para el filtrado superior (Pestañas)
    private lateinit var filterAdapter: FilterAdapter
    // Adaptador para la lista de gastos (Dentro de la vista General)
    private lateinit var categoryAdapter: CategoryAdapter

    private val ID_GENERAL = 1
    private val ID_PRESUPUESTOS = 2
    private val ID_METAS = 3
    private val ID_HISTORIAL = 4

    override fun inflateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentMoneyBinding {
        return FragmentMoneyBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Configurar pestañas superiores
        setupFilterRecyclerView()

        // 2. Configurar la vista "General" con sus datos mock
        setupGeneralView()

        // 3. Inicializar visibilidad
        updateContentVisibility(ID_GENERAL)

        setupListeners()
    }

    override fun observe() {
        // Aquí observarías al ViewModel en el futuro
    }

    private fun setupListeners() {
        // Listener ejemplo para el botón "Ver todo" dentro del include general
        binding.viewGeneral.btnSeeAll.setOnClickListener {
            // Navegar a detalle
        }
    }

    // === AQUÍ ESTÁ LA LÓGICA DE DATOS MOCK ===
    private fun setupGeneralView() {
        // Datos Mock calcados de la imagen
        val mockCategories = listOf(
            CategorySendModel(
                id = 1,
                name = "Alimentación",
                amount = "380,00 €",
                percentage = "98.7% del total",
                progress = 98,
                iconRes = R.drawable.ic_health, // Asegúrate de tener este icono o usa ic_restaurant
                colorHex = "#F57C00" // Naranja
            ),
            CategorySendModel(
                id = 2,
                name = "Entretenimiento",
                amount = "3,00 €",
                percentage = "0.8% del total",
                progress = 5,
                iconRes = R.drawable.ic_book, // Asegúrate de tener este icono
                colorHex = "#8B3DFF" // Morado
            ),
            CategorySendModel(
                id = 3,
                name = "Educación",
                amount = "2,00 €",
                percentage = "0.5% del total",
                progress = 2,
                iconRes = R.drawable.ic_book, // Asegúrate de tener este icono
                colorHex = "#2962FF" // Azul
            )
        )

        categoryAdapter = CategoryAdapter(mockCategories)

        // Accedemos al RecyclerView que está DENTRO del include viewGeneral
        binding.viewGeneral.rvCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false // Importante para scroll suave dentro del NestedScrollView padre
        }
    }

    private fun setupFilterRecyclerView() {
        val filters = listOf(
            FilterModel(ID_GENERAL, "General", isSelected = true),
            FilterModel(ID_PRESUPUESTOS, "Presupuestos", count = 2),
            FilterModel(ID_METAS, "Metas"), // Icono opcional
            FilterModel(ID_HISTORIAL, "Historial")
        )

        filterAdapter = FilterAdapter(filters) { selectedFilter ->
            updateContentVisibility(selectedFilter.id)
        }

        binding.rvFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
            itemAnimator = null
        }
    }

    private fun updateContentVisibility(filterId: Int) {
        binding.apply {
            // Nota: Al usar <include> con ID en el XML padre, ViewBinding genera la propiedad
            // que apunta a ese binding hijo. Accedemos a .root para cambiar la visibilidad de todo el layout.
            viewGeneral.root.isVisible = filterId == ID_GENERAL
            viewBudgets.root.isVisible = filterId == ID_PRESUPUESTOS
            viewGoals.root.isVisible = filterId == ID_METAS
            viewHistory.root.isVisible = filterId == ID_HISTORIAL
        }
    }
}