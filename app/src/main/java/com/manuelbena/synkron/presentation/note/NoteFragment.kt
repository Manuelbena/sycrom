package com.manuelbena.synkron.presentation.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNoteBinding
import com.manuelbena.synkron.presentation.models.FilterModel
import com.manuelbena.synkron.presentation.note.adapter.FilterAdapter
import com.manuelbena.synkron.presentation.note.adapter.SuperPlanManagementAdapter
import com.manuelbena.synkron.presentation.note.adapter.TaskManagementAdapter
import com.manuelbena.synkron.presentation.superTask.SuperTaskBottomSheet
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NoteFragment : BaseFragment<FragmentNoteBinding, NoteViewModel>() {

    override val viewModel: NoteViewModel by viewModels()

    private lateinit var tasksAdapter: TaskManagementAdapter
    private lateinit var superPlanAdapter: SuperPlanManagementAdapter
    private lateinit var filterAdapter: FilterAdapter

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNoteBinding {
        return FragmentNoteBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        setupAdapters()
        setupFilters()
    }

    private fun setupAdapters() {
        tasksAdapter = TaskManagementAdapter { task ->
            TaskDetailBottomSheet.newInstance(task).show(childFragmentManager, "TaskDetail")
        }

        superPlanAdapter = SuperPlanManagementAdapter { superTask ->
            SuperTaskBottomSheet.newInstance(superTask).show(childFragmentManager, "SuperTaskSheet")
        }

        // Asignamos por defecto el adapter de tareas normales
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tasksAdapter
        }
    }

    private fun setupFilters() {
        // 1. UNIFICACIÓN: Usamos directamente tus categorías de UI, no las de Base de Datos
        val initialFilters = listOf(
            FilterModel(1, "Pendientes", 0, R.drawable.ic_health, isSelected = true),
            FilterModel(2, "Super Planes", 0, R.drawable.ic_health, isSelected = false),
            FilterModel(3, "Guardados", 0, R.drawable.ic_health, isSelected = false)
        )

        filterAdapter = FilterAdapter(initialFilters) { selectedFilter ->
            viewModel.filterByCategory(selectedFilter.name)

            binding.apply {
                if (selectedFilter.name == "Super Planes") {
                    btnCreateSuperPlan.isVisible = true
                    // Swapping seguro:
                    if (rvTasks.adapter != superPlanAdapter) rvTasks.adapter = superPlanAdapter

                    val plans = viewModel.superPlans.value
                    superPlanAdapter.submitList(plans)

                    lyEmptyState.isVisible = plans.isEmpty()
                    rvTasks.isVisible = plans.isNotEmpty()
                    tvSummaryText.text = if (plans.size == 1) "Tienes 1 Super Plan" else "Tienes ${plans.size} Super Planes"
                } else {
                    btnCreateSuperPlan.isVisible = false
                    // Swapping seguro: Volvemos a las tareas normales
                    if (rvTasks.adapter != tasksAdapter) rvTasks.adapter = tasksAdapter

                    val tasks = viewModel.uiTasks.value
                    tasksAdapter.submitList(tasks)

                    lyEmptyState.isVisible = tasks.isEmpty()
                    rvTasks.isVisible = tasks.isNotEmpty()
                    tvSummaryText.text = if (tasks.size == 1) "Tienes 1 tarea" else "Tienes ${tasks.size} tareas"
                }
            }
        }

        binding.rvFilters.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
            itemAnimator = null
        }
    }

    private fun updateFiltersUI() {
        // Solo actualizamos los contadores para no romper el estado de "isSelected"
        val pendingCount = viewModel.getCountForCategory("Pendientes")
        val superPlanCount = viewModel.superPlans.value.size
        val currentName = viewModel.currentFilterName

        val updatedFilters = listOf(
            FilterModel(1, "Pendientes", pendingCount, R.drawable.ic_health, isSelected = (currentName == "Pendientes")),
            FilterModel(2, "Super Planes", superPlanCount, R.drawable.ic_health, isSelected = (currentName == "Super Planes")),
            FilterModel(3, "Guardados", 0, R.drawable.ic_health, isSelected = (currentName == "Guardados"))
        )

        filterAdapter.updateList(updatedFilters)
    }

    override fun observe() {
        // CORRECCIÓN: Evitamos memory leaks con repeatOnLifecycle
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.uiTasks.collectLatest { tasks ->
                        if (viewModel.currentFilterName != "Super Planes") {
                            tasksAdapter.submitList(tasks)
                            binding.lyEmptyState.isVisible = tasks.isEmpty()
                            binding.rvTasks.isVisible = tasks.isNotEmpty()
                            binding.tvSummaryText.text = if (tasks.size == 1) "Tienes 1 tarea pendiente" else "Tienes ${tasks.size} tareas pendientes"
                        }
                        updateFiltersUI()
                    }
                }

                launch {
                    viewModel.superPlans.collectLatest { plans ->
                        if (viewModel.currentFilterName == "Super Planes") {
                            superPlanAdapter.submitList(plans)
                            binding.lyEmptyState.isVisible = plans.isEmpty()
                            binding.rvTasks.isVisible = plans.isNotEmpty()
                            binding.tvSummaryText.text = if (plans.size == 1) "Tienes 1 Super Plan" else "Tienes ${plans.size} Super Planes"
                        }
                        updateFiltersUI()
                    }
                }
            }
        }
    }

    override fun setListener() {
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