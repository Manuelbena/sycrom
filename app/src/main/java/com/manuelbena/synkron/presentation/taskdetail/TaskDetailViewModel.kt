package com.manuelbena.synkron.presentation.taskdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.usecase.DeleteTaskUseCase
import com.manuelbena.synkron.domain.usecase.GetTaskTodayUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskTodayUseCase: GetTaskTodayUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _task = MutableLiveData<TaskDomain?>()
    val task: LiveData<TaskDomain?> = _task

    private val _shouldDismiss = MutableLiveData<Boolean>()
    val shouldDismiss: LiveData<Boolean> = _shouldDismiss

    init {
        val taskId = savedStateHandle.get<Int>("TASK_ID")
        if (taskId != null && taskId != 0) {
            getTask(taskId)
        }
    }

    fun getTask(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            getTaskTodayUseCase.invoke(LocalDate.now()).collect { taskList ->
                val foundTask = taskList.find { it.id == id }
                _task.postValue(foundTask)
            }
        }
    }

    fun togglePin() {
        _task.value?.let { currentTask ->
            val updatedTask = currentTask.copy(isPinned = !currentTask.isPinned)
            updateTask(updatedTask)
        }
    }

    fun toggleTaskCompletion(isChecked: Boolean) {
        _task.value?.let { currentTask ->
            if (currentTask.isDone != isChecked) {
                val updatedTask = currentTask.copy(isDone = isChecked)
                updateTask(updatedTask)
            }
        }
    }

    fun toggleSubtaskCompletion(subtask: SubTaskDomain, isChecked: Boolean) {
        _task.value?.let { currentTask ->
            val newSubtasks = currentTask.subTasks.map {
                if (it.id == subtask.id) it.copy(isDone = isChecked) else it
            }
            val updatedTask = currentTask.copy(subTasks = newSubtasks)
            updateTask(updatedTask)
        }
    }

    fun deleteReminder(reminder: GoogleEventReminder) {
        _task.value?.let { currentTask ->
            val currentReminders = currentTask.reminders.overrides.toMutableList()
            currentReminders.remove(reminder)

            // Creamos objeto actualizado
            val updatedReminders = currentTask.reminders.copy(overrides = currentReminders)
            val updatedTask = currentTask.copy(reminders = updatedReminders)

            updateTask(updatedTask)
        }
    }

    fun deleteTask(task: TaskDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTaskUseCase.invoke(task)
            _task.postValue(null)
            _shouldDismiss.postValue(true)
        }
    }

    private fun updateTask(task: TaskDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            updateTaskUseCase.invoke(task)
        }
    }
}