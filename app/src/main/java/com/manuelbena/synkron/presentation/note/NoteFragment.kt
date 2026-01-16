package com.manuelbena.synkron.presentation.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.manuelbena.synkron.R
import com.manuelbena.synkron.base.BaseFragment
import com.manuelbena.synkron.databinding.FragmentNoteBinding // O FragmentTaskBinding si ya le cambiaste el nombre
import com.manuelbena.synkron.presentation.models.FilterModel



import com.manuelbena.synkron.presentation.taskdetail.TaskDetailBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class NoteFragment : BaseFragment<FragmentNoteBinding, NoteViewModel>() {

    override val viewModel: NoteViewModel by viewModels()

    // Adaptadores
    private lateinit var tasksAdapter: TaskManagementAdapter
    private lateinit var filterAdapter: FilterAdapter

    override fun inflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentNoteBinding {
        // Asegúrate de que este binding apunte a tu XML nuevo (el que tiene el header de "Bandeja de Tareas")
        return FragmentNoteBinding.inflate(inflater, container, false)
    }

    override fun setUI() {
        setupFilters()
        setupTasksGrid()
    }

    private fun setupFilters() {
        // Datos de ejemplo para los filtros
        val filterList = listOf(
            FilterModel(1, "Pendientes", 5, true, R.drawable.ic_work),
            FilterModel(2, "Trabajo", 3, false, R.drawable.ic_work), // Asegúrate de tener icono
            FilterModel(3, "Personal", 2, false, R.drawable.ic_work)
        )

        // Inicializamos el FilterAdapter (que te pasé en la respuesta anterior)
        filterAdapter = FilterAdapter(filterList) { selectedFilter ->
            // Llamamos a la función que acabamos de crear en el ViewModel
            viewModel.filterByCategory(selectedFilter.name)
        }

        binding.rvFilters.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = filterAdapter
        }
    }

    private fun setupTasksGrid() {
        // Inicializamos el adaptador de TAREAS
        // Al usar ListAdapter, NO pasamos la lista en el constructor, solo el click listener
        tasksAdapter = TaskManagementAdapter { task ->
            val bottomSheet = TaskDetailBottomSheet.newInstance(task)
            bottomSheet.show(childFragmentManager, "TaskDetailBottomSheet")
        }

        binding.rvTasks.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            adapter = tasksAdapter
        }
    }

    override fun observe() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiTasks.collectLatest { tasks ->
                // 1. Actualizamos la lista de tareas (esto ya lo hacías)
                tasksAdapter.submitList(tasks)

                binding.lyEmptyState.isVisible = tasks.isEmpty()
                binding.rvTasks.isVisible = tasks.isNotEmpty()

                // ---------------------------------------------------------------
                // NUEVO: Sincronización visual de contadores (Banner y Filtros)
                // ---------------------------------------------------------------

                // A. Obtenemos el número REAL de pendientes (con la lógica corregida)
                val pendingCount = viewModel.getCountForCategory("Pendientes")

                // B. Actualizamos el Banner "Tienes X tareas pendientes"
                // Usa el plural/singular correctamente si quieres pulirlo
                val text = if (pendingCount == 1) "Tienes 1 tarea pendiente" else "Tienes $pendingCount tareas pendientes"
                binding.tvSummaryText.text = text

                // C. Actualizamos los números de los Filtros (Chips)
                // Reconstruimos la lista para que se actualicen los contadores (ej: Trabajo 3 -> Trabajo 2)
                val updatedFilters = listOf(
                    FilterModel(1, "Pendientes", pendingCount, iconRes = R.drawable.ic_note),
                    FilterModel(2, "Trabajo", viewModel.getCountForCategory("Trabajo"), iconRes = R.drawable.ic_work),
                    FilterModel(3, "Personal", viewModel.getCountForCategory("Personal"), iconRes = R.drawable.ic_work),
                    // ... añade aquí el resto de tus categorías fijas ...
                )

                // D. Mantenemos seleccionado el que estaba activo
                val currentName = viewModel.currentFilterName
                updatedFilters.forEach { it.isSelected = (it.name == currentName) }

                // E. Enviamos la nueva lista al adaptador de filtros
                filterAdapter.updateList(updatedFilters)
            }
        }
    }

    override fun setListener() {
        // Si decidiste quitar el SearchView del XML como en la imagen, borra esto.
        // Si lo mantienes, usa este código:
        /*
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterByQuery(newText ?: "")
                return true
            }
        })
        */
    }
}