package com.manuelbena.synkron.presentation.task

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.models.CategoryType
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
    private val taskRepository: ITaskRepository
) : BaseViewModel<TaskEvent>() {

    companion object {
        // Función auxiliar para obtener la siguiente hora en punto
        private fun getNextCleanHour(): Calendar {
            return Calendar.getInstance().apply {
                if (get(Calendar.MINUTE) > 0) {
                    add(Calendar.HOUR_OF_DAY, 1)
                }
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    // [CORRECCIÓN CLAVE] Inicialización Inteligente
    private val _state = MutableLiveData(
        TaskState(
            isAllDay = false,
            isNoDate = false,

            // 2. Horas "Limpias": Redondeamos a la siguiente hora en punto
            // (Ej: Si son las 10:25 -> Inicio 11:00, Fin 12:00)
            startTime = getNextCleanHour(),
            endTime = getNextCleanHour().apply { add(Calendar.HOUR_OF_DAY, 1) }
        )
    )
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
                // Preservamos la hora que tuviera configurada
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
                // [LÓGICA DE 1 HORA] Si cambia el inicio, empujamos el fin si es necesario
                var newEnd = current.endTime
                // Si el nuevo inicio es después o igual al fin, o si la duración era 0...
                // Forzamos que el fin sea Start + 1h
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

            is TaskEvent.OnTaskTypeChanged -> {
                // event.tabIndex -> 0: Evento, 1: Todo el día, 2: Sin Fecha
                updateState {
                    copy(isAllDay = event.tabIndex == 1, isNoDate = event.tabIndex == 2)
                }
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

    private fun loadTaskFromId(taskId: Int) { /* Implementar si es necesario */ }

    private fun loadTask(task: TaskDomain) {
        val startCal = googleDateToCalendar(task.start)
        val endCal = googleDateToCalendar(task.end)

        // Recuperar categoría real
        val catId = CategoryType.getAll().find { it.googleColorId == task.colorId }?.title ?: "other"


        updateState {
            copy(
                id = task.id,
                title = task.summary,
                description = task.description ?: "",
                location = task.location ?: "",

                category = task.typeTask,
                colorId = catId,
                priority = task.priority,

                selectedDate = startCal,
                startTime = startCal,
                endTime = endCal,

                // Detectamos estados
                isAllDay = task.start?.dateTime == null && !task.start?.date.isNullOrEmpty(),
                isNoDate = task.start == null,

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
                // Validación Todo el día único
                if (s.isAllDay) {
                    val dateToCheck = java.time.LocalDateTime.ofInstant(
                        s.selectedDate.toInstant(),
                        java.time.ZoneId.systemDefault()
                    ).toLocalDate()

                    val exists = taskRepository.hasAllDayTaskOnDate(dateToCheck, s.id)
                    if (exists) {
                        updateState { copy(isLoading = false) }
                        _effect.value = TaskEffect.ShowMessage("⚠️ Ya tienes una tarea de 'Todo el día' para esta fecha.")
                        return@launch
                    }
                }

                // Preparación de calendarios
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

                // Generación de fechas según tipo
                val (googleStart, googleEnd) = when {
                    s.isNoDate -> Pair(null, null)
                    s.isAllDay -> Pair(
                        calendarToGoogleDateAllDay(finalStart),
                        calendarToGoogleDateAllDay(finalEnd)
                    )
                    else -> Pair(
                        calendarToGoogleDateTimed(finalStart),
                        calendarToGoogleDateTimed(finalEnd)
                    )
                }



                val task = TaskDomain(
                    id = s.id,
                    typeTask = s.category, // <--- GUARDAMOS EL ID ("work"), NO EL NOMBRE
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
                    isArchived = s.isNoDate,
                    isActive = true, isDone = false, isDeleted = false, isPinned = false,
                    transparency = "opaque", conferenceLink = ""
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

    // --- Helpers ---
    private fun calendarToGoogleDateTimed(cal: Calendar): GoogleEventDateTime {
        return GoogleEventDateTime(dateTime = cal.timeInMillis, date = null, timeZone = TimeZone.getDefault().id)
    }

    private fun calendarToGoogleDateAllDay(cal: Calendar): GoogleEventDateTime {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return GoogleEventDateTime(dateTime = null, date = format.format(cal.time), timeZone = TimeZone.getDefault().id)
    }

    private fun googleDateToCalendar(gDate: GoogleEventDateTime?): Calendar {
        val cal = Calendar.getInstance()
        if (gDate != null) {
            if (gDate.dateTime != null) {
                cal.timeInMillis = gDate.dateTime!!
            } else if (!gDate.date.isNullOrEmpty()) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = format.parse(gDate.date)
                    if (date != null) cal.time = date
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        return cal
    }

    private fun updateState(update: TaskState.() -> TaskState) {
        _state.value = _state.value?.update()
    }
}