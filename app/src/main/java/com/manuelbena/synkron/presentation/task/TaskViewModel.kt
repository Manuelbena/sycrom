package com.manuelbena.synkron.presentation.task

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.RecurrenceType
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val insertTaskUseCase: InsertNewTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val taskRepository: ITaskRepository
) : BaseViewModel<TaskEvent>() {

    private val _state = MutableLiveData(TaskState())
    val state: LiveData<TaskState> = _state

    private val _effect = SingleLiveEvent<TaskEffect>()
    val effect: LiveData<TaskEffect> = _effect

    fun onEvent(event: TaskEvent) {
        val current = _state.value ?: TaskState()

        when (event) {
            is TaskEvent.OnLoadTaskById -> loadTaskFromId(event.taskId)
            is TaskEvent.OnTitleChange -> updateState { copy(title = event.title) }
            is TaskEvent.OnDescriptionChange -> updateState { copy(description = event.desc) }
            is TaskEvent.OnLocationChange -> updateState { copy(location = event.loc) }

            is TaskEvent.OnDateClicked -> _effect.value = TaskEffect.ShowDatePicker(current.selectedDate.timeInMillis)
            is TaskEvent.OnDateSelected -> {
                val newCal = Calendar.getInstance().apply { timeInMillis = event.date }
                newCal.set(Calendar.HOUR_OF_DAY, current.selectedDate.get(Calendar.HOUR_OF_DAY))
                newCal.set(Calendar.MINUTE, current.selectedDate.get(Calendar.MINUTE))
                newCal.set(Calendar.SECOND, 0)
                updateState { copy(selectedDate = newCal) }
            }

            is TaskEvent.OnStartTimeClicked -> _effect.value = TaskEffect.ShowTimePicker(current.startTime, true)
            is TaskEvent.OnStartTimeSelected -> {
                val newStart = (current.startTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                var newEnd = current.endTime
                if (newStart.after(newEnd)) {
                    newEnd = (newStart.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
                }
                updateState { copy(startTime = newStart, endTime = newEnd) }
            }

            is TaskEvent.OnEndTimeClicked -> _effect.value = TaskEffect.ShowTimePicker(current.endTime, false)
            is TaskEvent.OnEndTimeSelected -> {
                val newEnd = (current.endTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                updateState { copy(endTime = newEnd) }
            }

            is TaskEvent.OnTaskTypeChanged -> {
                updateState { copy(isAllDay = event.tabIndex == 1, isNoDate = event.tabIndex == 2) }
            }

            is TaskEvent.OnAddReminderClicked -> _effect.value = TaskEffect.ShowReminderDialog
            is TaskEvent.OnAddReminder -> {
                val newList = current.reminders + event.reminder
                updateState { copy(reminders = newList) }
            }
            is TaskEvent.OnRemoveReminder -> {
                val toRemove = current.reminders.find { it.method == event.reminder.method && it.minutes == event.reminder.minutes }
                if (toRemove != null) updateState { copy(reminders = reminders - toRemove) }
            }
            is TaskEvent.OnUpdateReminders -> updateState { copy(reminders = event.reminders) }

            is TaskEvent.OnCategorySelectorClicked -> _effect.value = TaskEffect.ShowCategoryDialog
            is TaskEvent.OnCategorySelected -> updateState { copy(category = event.category, colorId = event.colorId) }

            is TaskEvent.OnPrioritySelectorClicked -> _effect.value = TaskEffect.ShowPriorityDialog
            is TaskEvent.OnPrioritySelected -> updateState { copy(priority = event.priority) }

            is TaskEvent.OnRecurrenceSelectorClicked -> _effect.value = TaskEffect.ShowRecurrenceDialog
            is TaskEvent.OnRecurrenceTypeSelected -> updateState { copy(recurrenceType = event.type) }
            is TaskEvent.OnRecurrenceDayToggled -> {
                val newSet = current.selectedRecurrenceDays.toMutableSet()
                if (event.isSelected) newSet.add(event.day) else newSet.remove(event.day)
                updateState { copy(selectedRecurrenceDays = newSet) }
            }

            is TaskEvent.OnAddSubTask -> {
                val newSub = SubTaskDomain(id = UUID.randomUUID().toString(), title = event.text, isDone = false)
                updateState { copy(subTasks = subTasks + newSub) }
            }
            is TaskEvent.OnRemoveSubTask -> updateState { copy(subTasks = subTasks - event.item) }
            is TaskEvent.OnReorderSubTasks -> updateState { copy(subTasks = event.items) }

            is TaskEvent.OnSaveClicked -> saveTask()
            is TaskEvent.OnCancelClicked -> _effect.value = TaskEffect.NavigateBack
            is TaskEvent.OnLoadTaskForEdit -> loadTask(event.task)
        }
    }

    private fun loadTaskFromId(taskId: Int) {
        viewModelScope.launch {
            try {
                updateState { copy(isLoading = true) }
                val task = taskRepository.getTaskById(taskId)
                if (task != null) {
                    Log.d("SYCROM_DEBUG", "VM: Tarea cargada: ${task.summary}")
                    loadTask(task) // Reutilizamos la lógica de mapeo
                } else {
                    _effect.value = TaskEffect.ShowMessage("No se encontró la tarea")
                }
                updateState { copy(isLoading = false) }
            } catch (e: Exception) {
                Log.e("SYCROM_DEBUG", "Error cargando tarea: ${e.message}")
                updateState { copy(isLoading = false) }
            }
        }
    }

    private fun saveTask() {
        val s = _state.value ?: return
        if (s.title.isBlank()) {
            _effect.value = TaskEffect.ShowMessage("Falta título")
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                val finalStart = (s.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, s.startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, s.startTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }
                val finalEnd = (s.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, s.endTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, s.endTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }

                val task = TaskDomain(
                    id = s.id,
                    typeTask = s.category,
                    summary = s.title,
                    description = s.description,
                    location = s.location,
                    start = if (!s.isNoDate) calendarToGoogleDate(finalStart) else null,
                    end = if (!s.isNoDate) calendarToGoogleDate(finalEnd) else null,
                    colorId = s.colorId,
                    priority = s.priority,
                    subTasks = s.subTasks,
                    reminders = GoogleEventReminders(overrides = s.reminders),
                   // synkronRecurrence = s.recurrenceType,
                    synkronRecurrenceDays = s.selectedRecurrenceDays.toList(),
                    isActive = true, isDone = false, isDeleted = false, isArchived = false, isPinned = false, transparency = "opaque", conferenceLink = null
                )

                if (s.id == 0) insertTaskUseCase(task) else updateTaskUseCase(task)
                _effect.value = TaskEffect.NavigateBack
            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                _effect.value = TaskEffect.ShowMessage("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadTask(task: TaskDomain) {
        val startCal = googleDateToCalendar(task.start)
        val endCal = googleDateToCalendar(task.end)
        updateState {
            copy(
                id = task.id,
                title = task.summary,
                description = task.description ?: "",
                location = task.location ?: "",
                category = task.typeTask,
                priority = task.priority,
                selectedDate = startCal,
                startTime = startCal,
                endTime = endCal,
                subTasks = task.subTasks,
                reminders = task.reminders.overrides,
                //recurrenceType = task.synkronRecurrence,
                selectedRecurrenceDays = task.synkronRecurrenceDays.toSet()
            )
        }
    }

    private fun calendarToGoogleDate(cal: Calendar): GoogleEventDateTime {
        val zdt = ZonedDateTime.ofInstant(cal.toInstant(), ZoneId.systemDefault())
        return GoogleEventDateTime(dateTime = zdt, timeZone = ZoneId.systemDefault().id)
    }

    private fun googleDateToCalendar(gDate: GoogleEventDateTime?): Calendar {
        val cal = Calendar.getInstance()
        if (gDate?.dateTime != null) {
            cal.time = Date.from(gDate.dateTime.toInstant())
        }
        return cal
    }

    private fun updateState(update: TaskState.() -> TaskState) {
        _state.value = _state.value?.update()
    }
}