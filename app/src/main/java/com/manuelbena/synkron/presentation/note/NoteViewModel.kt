package com.manuelbena.synkron.presentation.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.interfaces.ISuperTaskRepository
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.SuperTaskModel
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NoteViewModel @Inject constructor(
    private val taskRepository: ITaskRepository,
    private val superTaskRepository: ISuperTaskRepository
) : ViewModel() {

    var currentFilterName: String = "Pendientes"
        private set

    private val _allTasks = MutableStateFlow<List<TaskDomain>>(emptyList())
    private val _uiTasks = MutableStateFlow<List<TaskDomain>>(emptyList())
    val uiTasks: StateFlow<List<TaskDomain>> = _uiTasks.asStateFlow()

    private var currentQuery: String = ""

    // Flujo de Super Planes (Cargamos los del día o todos, según tu lógica de negocio)
    val superPlans: StateFlow<List<SuperTaskModel>> = superTaskRepository
        .getAllSuperTasksForDate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        observeAllTasks()
    }

    private fun observeAllTasks() {
        viewModelScope.launch {
            taskRepository.getAllTasks().collectLatest { dbTasks ->
                // Guardamos la lista ordenada por fecha
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
                tasks.filter { !it.isDone }
                    .filter { !it.summary.contains("Cumpleaños", ignoreCase = true) }
                    // Agrupamos por parentId si existe, o por ID si es única
                    .distinctBy { it.parentId ?: it.id }
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
        currentFilterName = categoryName
        applyFilters()
    }

    fun getCountForCategory(categoryName: String): Int {
        val list = _allTasks.value
        return when (categoryName) {
            "Todos" -> list.size

            "Pendientes" -> {
                list.filter { !it.isDone }
                    .filter { !it.summary.contains("Cumpleaños", ignoreCase = true) }
                    .distinctBy { it.parentId ?: it.id }
                    .count()
            }

            "Completadas" -> list.count { it.isDone }
            else -> list.count { it.typeTask.equals(categoryName, ignoreCase = true) }
        }
    }

    // --- MÉTODOS PARA SUPER TAREAS (ESTO ES LO QUE FALTABA) ---

    fun updateSuperTask(task: SuperTaskModel) {
        viewModelScope.launch {
            try {
                // Guardamos en la base de datos local
                superTaskRepository.saveSuperTask(task)
                // No necesitamos actualizar _uiState manualmente porque 'superPlans'
                // es un Flow conectado a la BD, así que se actualizará solo.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}