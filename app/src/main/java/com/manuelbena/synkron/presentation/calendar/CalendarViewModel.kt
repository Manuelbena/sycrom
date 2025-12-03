package com.manuelbena.synkron.presentation.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: ITaskRepository
) : BaseViewModel<Unit>() {

    private val _tasks = MutableLiveData<List<TaskDomain>>()
    val tasks: LiveData<List<TaskDomain>> = _tasks

    init {
        // Cargar tareas de hoy al iniciar
        getTasks(LocalDate.now())
    }

    fun getTasks(date: LocalDate) {
        viewModelScope.launch {
            taskRepository.getTasksForDate(date)
                .catch { e -> e.printStackTrace() }
                .collect { taskList ->
                    _tasks.value = taskList
                }
        }
    }

     fun onEvent(event: Unit) {}
}