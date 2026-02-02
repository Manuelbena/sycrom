package com.manuelbena.synkron.presentation.note

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNoteBinding
import com.manuelbena.synkron.presentation.models.FilterModel
import com.manuelbena.synkron.presentation.note.adapter.FilterAdapter
import com.manuelbena.synkron.presentation.note.adapter.SuperPlanManagementAdapter

import com.manuelbena.synkron.presentation.superTask.SuperTaskBottomSheet
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NoteFragment : BaseFragment<FragmentNoteBinding, NoteViewModel>() {

    override val viewModel: NoteViewModel by viewModels()

    // Adaptadores
    private lateinit var tasksAdapter: TaskManagementAdapter
    private lateinit var superPlanAdapter: SuperPlanManagementAdapter

    // Adaptador de filtros
    private lateinit var filterAdapter: FilterAdapter

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNoteBinding {
        return FragmentNoteBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        setupAdapters()
        setupFilters()
    }

    private fun setupAdapters() {
        // 1. Adapter Tareas Normales (Grilla)
        tasksAdapter = TaskManagementAdapter { task ->
            val bottomSheet = TaskDetailBottomSheet.newInstance(task)
            bottomSheet.show(childFragmentManager, "TaskDetailBottomSheet")
        }

        // 2. Adapter Super Planes (Lista Vertical)
        superPlanAdapter = SuperPlanManagementAdapter { superTask ->
            // Al hacer click, abrimos el BottomSheet de Super Tareas
            val bottomSheet = SuperTaskBottomSheet.newInstance(superTask)
            bottomSheet.show(childFragmentManager, "SuperTaskSheet")
        }
    }

    private fun setupFilters() {
        // Inicializamos el FilterAdapter con una lista vacía por ahora
        filterAdapter = FilterAdapter(emptyList()) { selectedFilter ->

            // --- LÓGICA DE CAMBIO DE VISTA ---
            viewModel.filterByCategory(selectedFilter.name)

            if (selectedFilter.name== "Super Planes") {
            // MODO SUPER PLANES
            binding.btnCreateSuperPlan.isVisible = true
            binding.rvTasks.layoutManager = LinearLayoutManager(context)
            binding.rvTasks.adapter = superPlanAdapter
            binding.tvSummaryText.text = "Gestiona tus rutinas complejas"

        } else {
            // MODO TAREAS NORMALES
            binding.btnCreateSuperPlan.isVisible = false
            binding.rvTasks.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            binding.rvTasks.adapter = tasksAdapter
        }

            // Actualizamos visualmente los chips
            updateFiltersUI()
        }

        binding.rvFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }
    }

    private fun updateFiltersUI() {
        val pendingCount = viewModel.getCountForCategory("Pendientes")
        val superPlanCount = viewModel.getSuperPlanCount()
        val currentName = viewModel.currentFilterName

        // --- AQUÍ ESTABA EL ERROR PROBABLEMENTE ---
        // Asegúrate de que no haya comas extra al final
        val updatedFilters = listOf(
            FilterModel(1, "Pendientes", pendingCount, isSelected = (currentName == "Pendientes"), iconRes = R.drawable.ic_health),
            FilterModel(2, "Super Planes", superPlanCount, isSelected = (currentName == "Super Planes"), iconRes = R.drawable.ic_health), // Usa ic_health o ic_flash
            FilterModel(3, "Guardados", 0, isSelected = (currentName == "Guardados"), iconRes = R.drawable.ic_health)
        )

        filterAdapter.updateList(updatedFilters)
    }

    override fun observe() {
        lifecycleScope.launchWhenStarted {
            // 1. Observar Tareas Normales
            launch {
                viewModel.uiTasks.collectLatest { tasks ->
                    // Solo actualizamos si NO estamos viendo super planes
                    if (viewModel.currentFilterName != "Super Planes") {
                        tasksAdapter.submitList(tasks)

                        binding.lyEmptyState.isVisible = tasks.isEmpty()
                        binding.rvTasks.isVisible = tasks.isNotEmpty()

                        val count = tasks.size
                        binding.tvSummaryText.text = if (count == 1) "Tienes 1 tarea pendiente" else "Tienes $count tareas pendientes"
                    }
                    // Siempre actualizamos los contadores de los chips
                    updateFiltersUI()
                }
            }

            // 2. Observar Super Planes
            launch {
                viewModel.superPlans.collectLatest { plans ->
                    // Solo actualizamos si ESTAMOS viendo super planes
                    if (viewModel.currentFilterName == "Super Planes") {
                        superPlanAdapter.submitList(plans)

                        binding.lyEmptyState.isVisible = plans.isEmpty()
                        binding.rvTasks.isVisible = plans.isNotEmpty()
                    }
                    updateFiltersUI()
                }
            }
        }
    }

    override fun setListener() {
        // Botón "+ Crear Super Plan"
        binding.btnCreateSuperPlan.setOnClickListener {
            val createSheet = CreateSuperTaskBottomSheet()
            createSheet.onSaveListener = { newSuperTask ->
                viewModel.updateSuperTask(newSuperTask)
                Snackbar.make(binding.root, "Plan '${newSuperTask.title}' creado", Snackbar.LENGTH_SHORT).show()
            }
            createSheet.show(childFragmentManager, "CreateSuperPlan")
        }
    }
}