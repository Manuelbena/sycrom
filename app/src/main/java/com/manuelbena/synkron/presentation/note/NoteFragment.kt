package com.manuelbena.synkron.presentation.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNoteBinding
import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class NoteFragment : BaseFragment<FragmentNoteBinding, NoteViewModel>() {

    override val viewModel: NoteViewModel by viewModels()

    private lateinit var tasksAdapter: TaskManagementAdapter
    private lateinit var filterAdapter: FilterAdapter

    // Categorías fijas para el filtro
    private val categories = listOf("Todos", "Trabajo", "Personal", "Salud", "Estudios", "Finanzas")

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNoteBinding {
        return FragmentNoteBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        setupFilters()
        setupTasksGrid()
    }

    private fun setupFilters() {
        // Reutilizamos el FilterAdapter (definido abajo para conveniencia)
        filterAdapter = FilterAdapter(categories) { selectedCategory ->
            viewModel.filterByCategory(selectedCategory)
        }

        binding.rvFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }
    }

    private fun setupTasksGrid() {
        tasksAdapter = TaskManagementAdapter { task ->
            // Abrir detalle de tarea
            val bottomSheet = TaskDetailBottomSheet.newInstance(task)
            bottomSheet.show(childFragmentManager, "TaskDetailBottomSheet")
        }

        binding.rvTasks.apply {
            // SPAN COUNT = 2 (Dos columnas tipo mosaico)
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = tasksAdapter
            // Animación al cargar
            scheduleLayoutAnimation()
        }
    }

    override fun observe() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiTasks.collectLatest { tasks ->
                tasksAdapter.submitList(tasks)

                binding.lyEmptyState.isVisible = tasks.isEmpty()
                binding.rvTasks.isVisible = tasks.isNotEmpty()
            }
        }
    }

    override fun setListener() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterByQuery(newText ?: "")
                return true
            }
        })
    }
}