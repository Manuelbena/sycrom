package com.manuelbena.synkron.presentation.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import com.manuelbena.synkron.presentation.util.toGoogleEventDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val insertTaskUseCase: InsertNewTaskUseCase
) : BaseViewModel<TaskEvent>() {

    private val _state = MutableLiveData(TaskState())
    val state: LiveData<TaskState> = _state

    private val _effect = SingleLiveEvent<TaskEffect>()
    val effect: LiveData<TaskEffect> = _effect

    fun onEvent(event: TaskEvent) {
        val current = _state.value ?: TaskState()

        when (event) {
            is TaskEvent.OnTitleChange -> updateState { copy(title = event.title) }
            is TaskEvent.OnDescriptionChange -> updateState { copy(description = event.desc) }
            is TaskEvent.OnLocationChange -> updateState { copy(location = event.loc) }

            is TaskEvent.OnTaskTypeChanged -> {
                // 0: Planificado, 1: Todo el día, 2: Sin fecha
                updateState {
                    copy(
                        isAllDay = event.tabIndex == 1,
                        isNoDate = event.tabIndex == 2
                    )
                }
            }

            is TaskEvent.OnDateSelected -> {
                val newCal = Calendar.getInstance().apply { timeInMillis = event.date }
                // Mantenemos la hora que ya tenía
                newCal.set(Calendar.HOUR_OF_DAY, current.selectedDate.get(Calendar.HOUR_OF_DAY))
                newCal.set(Calendar.MINUTE, current.selectedDate.get(Calendar.MINUTE))
                updateState { copy(selectedDate = newCal) }
            }

            is TaskEvent.OnStartTimeSelected -> {
                val newStart = (current.startTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                }
                var newEnd = current.endTime
                if (newStart.after(newEnd)) {
                    newEnd = (newStart.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
                }
                updateState { copy(startTime = newStart, endTime = newEnd) }
            }

            is TaskEvent.OnEndTimeSelected -> {
                val newEnd = (current.endTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                }
                if (newEnd.before(current.startTime)) {
                    _effect.value = TaskEffect.ShowMessage("La hora fin no puede ser anterior al inicio")
                } else {
                    updateState { copy(endTime = newEnd) }
                }
            }

            is TaskEvent.OnAddSubTask -> {
                if (event.text.isNotBlank()) {
                    val sub = SubTaskDomain(
                        id = UUID.randomUUID().toString(),
                        title = event.text,
                        isDone = false
)
                    updateState { copy(subTasks = subTasks + sub) }
                }
            }
            is TaskEvent.OnRemoveSubTask -> updateState { copy(subTasks = subTasks - event.item) }

            is TaskEvent.OnAddReminderClicked -> _effect.value = TaskEffect.ShowReminderDialog
            is TaskEvent.OnAddReminder -> updateState { copy(reminders = reminders + event.reminder) }
            is TaskEvent.OnRemoveReminder -> updateState { copy(reminders = reminders - event.reminder) }

            is TaskEvent.OnCategorySelected -> updateState { copy(category = event.category, colorId = event.colorId) }
            is TaskEvent.OnPrioritySelected -> updateState { copy(priority = event.priority) }

            is TaskEvent.OnRecurrenceSelectorClicked -> _effect.value = TaskEffect.ShowRecurrenceDialog
            is TaskEvent.OnRecurrenceDayToggled -> {
                val newSet = current.selectedRecurrenceDays.toMutableSet()
                if (event.isSelected) newSet.add(event.day) else newSet.remove(event.day)
                updateState { copy(selectedRecurrenceDays = newSet) }
            }

            is TaskEvent.OnSaveClicked -> saveTask()
            is TaskEvent.OnCancelClicked -> _effect.value = TaskEffect.NavigateBack
        }
    }

    private fun saveTask() {
        val s = _state.value ?: return

        if (s.title.isBlank()) {
            _effect.value = TaskEffect.ShowMessage("Añade un título por favor")
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                // Preparar calendarios finales sincronizando fecha + hora
                val finalStart = (s.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, s.startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, s.startTime.get(Calendar.MINUTE))
                }

                val finalEnd = (s.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, s.endTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, s.endTime.get(Calendar.MINUTE))
                }

                val task = TaskDomain(
                    // --- Campos requeridos (sin valor por defecto en tu clase) ---
                    typeTask = s.category,
                    summary = s.title,
                    start = if (s.isNoDate) null else finalStart.toGoogleEventDateTime(), // CORREGIDO: Sin argumentos
                    end = if (s.isNoDate) null else finalEnd.toGoogleEventDateTime(),     // CORREGIDO: Sin argumentos

                    // --- Campos opcionales con valores del estado ---
                    id = 0L,
                    description = s.description,
                    location = s.location,
                    colorId = s.colorId,
                    priority = s.priority,
                    subTasks = s.subTasks,
                    reminders = GoogleEventReminders(overrides = s.reminders),
                    recurrence = generateRRule(s),

                    // --- Banderas internas (Pasadas explícitamente para evitar error) ---
                    isActive = true,
                    isDone = false,       // CORREGIDO: Valor explícito
                    isDeleted = false,    // CORREGIDO: Valor explícito
                    isArchived = false,   // CORREGIDO: Valor explícito
                    isPinned = false,     // CORREGIDO: Valor explícito

                    // --- Campos fijos/default de Google ---
                    transparency = "opaque",
                    attendees = emptyList(),
                    conferenceLink = null
                )

                insertTaskUseCase(task)

                _effect.value = TaskEffect.ShowMessage("Tarea creada")
                _effect.value = TaskEffect.NavigateBack
            } catch (e: Exception) {
                updateState { copy(isLoading = false, error = e.message) }
                _effect.value = TaskEffect.ShowMessage("Error: ${e.message}")
            }
        }
    }

    private fun updateState(update: TaskState.() -> TaskState) {
        _state.value = _state.value?.update()
    }

    private fun generateRRule(s: TaskState): List<String> {
        if (s.recurrenceType == RecurrenceType.NONE) return emptyList()
        return emptyList() // Implementar lógica real según necesidades
    }
}