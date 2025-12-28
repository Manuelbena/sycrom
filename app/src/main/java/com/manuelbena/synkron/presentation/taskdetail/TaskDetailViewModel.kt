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
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val getTaskTodayUseCase: GetTaskTodayUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val insertNewTaskUseCase: InsertNewTaskUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _task = MutableLiveData<TaskDomain?>()
    val task: LiveData<TaskDomain?> = _task

    private val _shouldDismiss = MutableLiveData<Boolean>()
    val shouldDismiss: LiveData<Boolean> = _shouldDismiss


    // 2. Añade o modifica esta función
    fun loadTaskDetails(t: TaskDomain) {
        // Esto fuerza que el observer de la UI salte INMEDIATAMENTE
        _task.value = t
    }

    init {
        // RECUPERAMOS EL OBJETO ENTERO
        val taskFromArgs = savedStateHandle.get<TaskDomain>("arg_task_obj")
        taskFromArgs?.let { loadTaskDetails(it) }

        if (taskFromArgs != null) {
            if (taskFromArgs.id == 0) {
                // TAREA NUEVA (IA): Usamos el objeto de memoria
                if (taskFromArgs.summary.isNotBlank()) {
                    _task.value = taskFromArgs
                } else {
                    _shouldDismiss.value = true
                }
            } else {
                // TAREA EXISTENTE: Mostramos inmediata y refrescamos de DB
                val taskDate = taskFromArgs.start?.dateTime?.let { millis ->
                    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                } ?: LocalDate.now()

                getTask(taskFromArgs.id, taskDate)
            }
        } else {
            // Fallback (por si acaso)
            val taskId = savedStateHandle.get<Int>("TASK_ID")
            if (taskId != null && taskId != 0) {
                // Si solo tenemos ID, asumimos hoy por defecto
                getTask(taskId, LocalDate.now())
            }
        }
    }

    fun getTask(id: Int, date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            getTaskTodayUseCase.invoke(date).collect { taskList ->
                val foundTask = taskList.find { it.id == id }

                // 🔥 FIX 3: Seguridad anti-nulos.
                // Solo actualizamos si la encontramos. Si no (ej. tarea fantasma),
                // nos quedamos con el objeto que pasamos por argumento que sí funciona.
                if (foundTask != null) {
                    _task.postValue(foundTask)
                }
            }
        }
    }

    fun deleteTaskInstance(task: TaskDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTaskUseCase.deleteInstance(task)
            closeScreen()
        }
    }

    fun deleteTaskSeries(task: TaskDomain) {
        viewModelScope.launch(Dispatchers.IO) {
            deleteTaskUseCase.deleteSeries(task)
            closeScreen()
        }
    }

    // Función auxiliar para cerrar y limpiar
    private fun closeScreen() {
        _task.postValue(null)
        _shouldDismiss.postValue(true)
    }

    // NUEVO: Método para confirmar la tarea de la IA
    fun saveOrUpdateTask() {
        val currentTask = _task.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (currentTask.id == 0) {
                // Insertar nueva (viene de IA)
                insertNewTaskUseCase.invoke(currentTask)
            } else {
                // Actualizar existente
                updateTaskUseCase.invoke(currentTask)
            }
            _shouldDismiss.postValue(true)
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
            if (task.id != 0) {
                deleteTaskUseCase.invoke(task)
            }
            _task.postValue(null)
            _shouldDismiss.postValue(true)
        }
    }

    fun updateTask(task: TaskDomain) {
        if (task.id == 0) {
            // Solo actualizamos memoria si aún no está guardada
            _task.value = task
        } else {
            // Guardamos en DB
            viewModelScope.launch(Dispatchers.IO) {
                updateTaskUseCase.invoke(task)
            }
        }
    }
}