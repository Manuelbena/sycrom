package com.manuelbena.synkron.presentation.activitys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val taskRepository: ITaskRepository
) : ViewModel() {

    val pendingCount: StateFlow<Int> = taskRepository.getAllTasks()
        .map { tasks ->
            tasks
                .filter { !it.isDone } // 1. Solo no completadas
                .filter { !it.summary.contains("Cumpleaños", ignoreCase = true) } // 2. Sin cumpleaños
                .distinctBy { it.summary ?: it.summary } // 3. AGRUPAR REPETIDAS (La clave)
                .count() // Contamos el resultado final
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}