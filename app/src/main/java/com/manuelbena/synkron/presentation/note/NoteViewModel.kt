package com.manuelbena.synkron.presentation.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val taskRepository: ITaskRepository
) : ViewModel() {

    var currentFilterName: String = "Pendientes"
        private set
    private val _allTasks = MutableStateFlow<List<TaskDomain>>(emptyList())
    private val _uiTasks = MutableStateFlow<List<TaskDomain>>(emptyList())
    val uiTasks: StateFlow<List<TaskDomain>> = _uiTasks.asStateFlow()

    private var currentQuery: String = ""

    init {
        observeAllTasks()
    }

    private fun observeAllTasks() {
        viewModelScope.launch {
            taskRepository.getAllTasks().collectLatest { dbTasks ->
                // Guardamos la lista ordenada por fecha para facilitar los filtros posteriores
                _allTasks.value = dbTasks.sortedBy { it.start?.dateTime ?: System.currentTimeMillis() }
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val tasks = _allTasks.value

        // 1. FILTRADO POR CATEGORÍA Y LÓGICA DE REPETIDAS
        val categoryFiltered = when (currentFilterName) {
            "Todos" -> tasks

            "Pendientes" -> {
                tasks.filter { !it.isDone } // Solo las que faltan por hacer
                    // AQUÍ ESTÁ EL TRUCO:
                    .filter { !it.summary.contains("Cumpleaños", ignoreCase = true) }
                    .distinctBy { task ->
                        // Si tiene parentId (es serie), usamos ese ID para agrupar.
                        // Si es null (tarea suelta), usamos su ID único para que no se oculte.
                        task.summary ?: task.summary

                    }
            }

            "Completadas" -> tasks.filter { it.isDone }

            else -> tasks.filter {
                it.typeTask.equals(currentFilterName, ignoreCase = true)
            }
        }

        // 2. FILTRADO POR BUSCADOR
        _uiTasks.value = if (currentQuery.isEmpty()) {
            categoryFiltered
        } else {
            categoryFiltered.filter {
                it.summary.contains(currentQuery, ignoreCase = true)
            }
        }
    }

    fun filterByCategory(categoryName: String) {
        currentFilterName = categoryName // Guardamos el nombre
        currentFilterName = categoryName     // Usamos la variable interna para lógica
        applyFilters()
    }

    // --- ESTA ES LA FUNCIÓN QUE DEBES CORREGIR ---
    fun getCountForCategory(categoryName: String): Int {
        val list = _allTasks.value
        return when (categoryName) {
            "Todos" -> list.size

            "Pendientes" -> {
                list.filter { !it.isDone }
                    // 1. AÑADIDO: Filtramos cumpleaños igual que en el Badge
                    .filter { !it.summary.contains("Cumpleaños", ignoreCase = true) }
                    // 2. AÑADIDO: Agrupamos repetidas igual que en el Badge
                    .distinctBy { it.summary ?: it.summary }
                    .count()
            }

            "Completadas" -> list.count { it.isDone }
            else -> list.count { it.typeTask.equals(categoryName, ignoreCase = true) }
        }
    }


}