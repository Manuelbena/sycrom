package com.manuelbena.synkron.presentation.task

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val insertTaskUseCase: InsertNewTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
) : BaseViewModel<TaskEvent>() {

    private val _state = MutableLiveData(TaskState())
    val state: LiveData<TaskState> = _state

    private val _effect = SingleLiveEvent<TaskEffect>()
    val effect: LiveData<TaskEffect> = _effect

    fun onEvent(event: TaskEvent) {
        val current = _state.value ?: TaskState()

        when (event) {
            is TaskEvent.OnLoadTaskById -> loadTaskFromId(event.taskId)
            is TaskEvent.OnLoadTaskForEdit -> loadTask(event.task)

            is TaskEvent.OnTitleChange -> updateState { copy(title = event.title) }
            is TaskEvent.OnDescriptionChange -> updateState { copy(description = event.desc) }
            is TaskEvent.OnLocationChange -> updateState { copy(location = event.loc) }
            is TaskEvent.OnDateClicked -> _effect.value = TaskEffect.ShowDatePicker(current.selectedDate.timeInMillis)
            is TaskEvent.OnDateSelected -> {
                val newCal = Calendar.getInstance().apply { timeInMillis = event.date }
                // Mantenemos la hora que tenía seleccionada, solo cambiamos el día
                newCal.set(Calendar.HOUR_OF_DAY, current.startTime.get(Calendar.HOUR_OF_DAY))
                newCal.set(Calendar.MINUTE, current.startTime.get(Calendar.MINUTE))
                newCal.set(Calendar.SECOND, 0)
                updateState { copy(selectedDate = newCal, startTime = newCal) }
            }
            is TaskEvent.OnStartTimeClicked -> _effect.value = TaskEffect.ShowTimePicker(current.startTime, true)
            is TaskEvent.OnStartTimeSelected -> {
                val newStart = (current.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                    set(Calendar.SECOND, 0)
                }
                // Regla: Si la hora de inicio cambia, y el fin queda antes, empujamos el fin 1 hora
                var newEnd = current.endTime
                if (newStart.after(newEnd) || newStart == newEnd) {
                    newEnd = (newStart.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
                }
                updateState { copy(startTime = newStart, endTime = newEnd) }
            }
            is TaskEvent.OnEndTimeClicked -> _effect.value = TaskEffect.ShowTimePicker(current.endTime, false)
            is TaskEvent.OnEndTimeSelected -> {
                val newEnd = (current.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                    set(Calendar.SECOND, 0)
                }
                updateState { copy(endTime = newEnd) }
            }
            is TaskEvent.OnTaskTypeChanged -> updateState {
                copy(isAllDay = event.tabIndex == 1, isNoDate = event.tabIndex == 2)
            }
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
            is TaskEvent.OnAddReminderClicked -> _effect.value = TaskEffect.ShowReminderDialog
            is TaskEvent.OnAddReminder -> updateState { copy(reminders = reminders + event.reminder) }
            is TaskEvent.OnRemoveReminder -> {
                val toRemove = current.reminders.find { it.method == event.reminder.method && it.minutes == event.reminder.minutes }
                if (toRemove != null) updateState { copy(reminders = reminders - toRemove) }
            }
            is TaskEvent.OnUpdateReminders -> updateState { copy(reminders = event.reminders) }
            is TaskEvent.OnSaveClicked -> saveTask()
            is TaskEvent.OnCancelClicked -> _effect.value = TaskEffect.NavigateBack
        }
    }

    private fun loadTaskFromId(taskId: Int) {
        // Implementación pendiente de conexión con repositorio
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
                colorId = task.colorId,
                priority = task.priority,

                selectedDate = startCal,
                startTime = startCal,
                endTime = endCal,

                // Detectamos el estado correcto al cargar
                isAllDay = task.start?.dateTime == null && !task.start?.date.isNullOrEmpty(),
                isNoDate = task.start == null, // O si isArchived es true, dependiendo de tu lógica

                subTasks = task.subTasks,
                reminders = task.reminders.overrides,
                selectedRecurrenceDays = task.synkronRecurrenceDays.toSet()
            )
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
                // Sincronizar fechas base con la fecha seleccionada
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

                // --- LÓGICA DE CONSTRUCCIÓN DE FECHAS SEGÚN TIPO ---
                val (googleStart, googleEnd) = when {
                    s.isNoDate -> Pair(null, null) // Caso: Sin Fecha
                    s.isAllDay -> Pair(
                        calendarToGoogleDateAllDay(finalStart), // Caso: Todo el día (String)
                        calendarToGoogleDateAllDay(finalEnd)
                    )
                    else -> Pair(
                        calendarToGoogleDateTimed(finalStart), // Caso: Hora exacta (Long)
                        calendarToGoogleDateTimed(finalEnd)
                    )
                }

                val task = TaskDomain(
                    id = s.id,
                    typeTask = s.category,
                    summary = s.title,
                    description = s.description,
                    location = s.location,
                    start = googleStart,
                    end = googleEnd,
                    colorId = s.colorId,
                    priority = s.priority,
                    subTasks = s.subTasks,
                    reminders = GoogleEventReminders(overrides = s.reminders),
                    synkronRecurrenceDays = s.selectedRecurrenceDays.toList(),

                    // --- AQUÍ ESTÁ LA LÓGICA QUE PEDISTE ---
                    // Si es "Sin Fecha" (isNoDate), la marcamos como Archivada automáticamente.
                    isArchived = s.isNoDate,

                    isActive = true,
                    isDone = false,
                    isDeleted = false,
                    isPinned = false,
                    transparency = "opaque",
                    conferenceLink = ""
                )

                if (s.id == 0) {
                    insertTaskUseCase(task)
                    _effect.value = TaskEffect.ShowMessage("Tarea guardada")
                } else {
                    updateTaskUseCase(task)
                    _effect.value = TaskEffect.ShowMessage("Tarea actualizada")
                }
                _effect.value = TaskEffect.NavigateBack

            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                _effect.value = TaskEffect.ShowMessage("Error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // --- HELPERS DE FECHA CORREGIDOS ---

    // 1. Para eventos con HORA (devuelve Long, date=null)
    private fun calendarToGoogleDateTimed(cal: Calendar): GoogleEventDateTime {
        return GoogleEventDateTime(
            dateTime = cal.timeInMillis,
            date = null, // Importante para que no sea 'Todo el día'
            timeZone = TimeZone.getDefault().id
        )
    }

    // 2. Para eventos TODO EL DÍA (devuelve String, dateTime=null)
    private fun calendarToGoogleDateAllDay(cal: Calendar): GoogleEventDateTime {
        // Formato requerido por Google Calendar API para 'date': "yyyy-MM-dd"
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return GoogleEventDateTime(
            dateTime = null, // Importante para que el Mapper detecte 'hour = -1'
            date = format.format(cal.time),
            timeZone = TimeZone.getDefault().id
        )
    }

    // 3. Carga genérica (ya la tenías bien, maneja ambos casos si tu modelo tiene los campos)
    private fun googleDateToCalendar(gDate: GoogleEventDateTime?): Calendar {
        val cal = Calendar.getInstance()
        if (gDate != null) {
            if (gDate.dateTime != null) {
                cal.timeInMillis = gDate.dateTime
            } else if (!gDate.date.isNullOrEmpty()) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = format.parse(gDate.date)
                    if (date != null) cal.time = date
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return cal
    }

    private fun updateState(update: TaskState.() -> TaskState) {
        _state.value = _state.value?.update()
    }
}