package com.manuelbena.synkron.presentation.note

import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val taskRepository: ITaskRepository
) : BaseViewModel<Unit>() {

    // Lista maestra (todas las tareas)
    private var allTasks: List<TaskDomain> = emptyList()

    // Lista visible (filtrada)
    private val _uiTasks = MutableStateFlow<List<TaskDomain>>(emptyList())
    val uiTasks: StateFlow<List<TaskDomain>> = _uiTasks.asStateFlow()

    init {
        loadAllTasks()
    }

    private fun loadAllTasks() {
        // Usamos executeFlow de tu BaseViewModel correctamente
        executeFlow(
            useCase = { taskRepository.getAllTasks() },
            onEach = { tasks ->
                // Filtramos solo las activas
                val activeTasks = tasks.filter { !it.isDeleted && !it.isArchived }
                allTasks = activeTasks
                // Aplicamos filtros actuales
                applyFilters()
            },
            onError = { /* Manejar error si es necesario */ }
        )
    }

    // Estado de los filtros
    private var currentQuery = ""
    private var currentCategory = "Todos"

    fun filterByQuery(query: String) {
        currentQuery = query
        applyFilters()
    }

    fun filterByCategory(category: String) {
        currentCategory = category
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = allTasks.filter { task ->
            // 1. Filtro de Categoría
            val matchesCategory = currentCategory == "Todos" ||
                    task.typeTask.equals(currentCategory, ignoreCase = true)

            // 2. Filtro de Texto (Título o Descripción)
            val matchesSearch = if (currentQuery.isEmpty()) true else {
                task.summary.contains(currentQuery, ignoreCase = true) ||
                        (task.description?.contains(currentQuery, ignoreCase = true) == true)
            }

            matchesCategory && matchesSearch
        }
        _uiTasks.value = filtered
    }
}