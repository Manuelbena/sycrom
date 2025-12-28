package com.manuelbena.synkron.presentation.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.RecurrenceType
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.models.CategoryType
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneId
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
        private fun getNextCleanHour(): Calendar {
            return Calendar.getInstance().apply {
                if (get(Calendar.MINUTE) > 0) add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    private val _state = MutableLiveData(
        TaskState(
            isAllDay = false,
            isNoDate = false,
            startTime = getNextCleanHour(),
            endTime = getNextCleanHour().apply { add(Calendar.HOUR_OF_DAY, 1) },
            // CORRECCIÓN: Por defecto una notificación popup de 30 min
            reminders = listOf(GoogleEventReminder(method = "popup", minutes = 30))
        )
    )
    val state: LiveData<TaskState> = _state

    private val _effect = SingleLiveEvent<TaskEffect>()
    val effect: LiveData<TaskEffect> = _effect

    fun onEvent(event: TaskEvent) {
        val current = _state.value ?: TaskState()

        when (event) {
            is TaskEvent.OnLoadTaskById -> loadTaskFromId(event.taskId) // Implementar si usas IDs
            is TaskEvent.OnLoadTaskForEdit -> loadTask(event.task)

            is TaskEvent.OnTitleChange -> updateState { copy(title = event.title) }
            is TaskEvent.OnDescriptionChange -> updateState { copy(description = event.desc) }
            is TaskEvent.OnLocationChange -> updateState { copy(location = event.loc) }
            is TaskEvent.OnDateClicked -> _effect.value = TaskEffect.ShowDatePicker(current.selectedDate.timeInMillis)

            is TaskEvent.OnDateSelected -> {
                val newCal = Calendar.getInstance().apply {
                    timeInMillis = event.date
                    // Mantenemos la hora actual
                    set(Calendar.HOUR_OF_DAY, current.startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, current.startTime.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                }
                updateState { copy(selectedDate = newCal, startTime = newCal) }
            }

            is TaskEvent.OnStartTimeClicked -> _effect.value = TaskEffect.ShowTimePicker(current.startTime, true)

            is TaskEvent.OnStartTimeSelected -> {
                val newStart = (current.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                    set(Calendar.SECOND, 0)
                }
                // Si el inicio es posterior al fin, movemos el fin 1h adelante
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

            is TaskEvent.OnTaskTypeChanged -> {
                updateState { copy(isAllDay = event.tabIndex == 1, isNoDate = event.tabIndex == 2) }
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

            // CORRECCIÓN: Al añadir, nos aseguramos que el método sea correcto
            is TaskEvent.OnAddReminder -> {
                // Aquí el fragmento ya nos pasa el objeto Reminder bien construido,
                // pero si quieres doble seguridad puedes mapear el string 'method' aquí.
                // Google Calendar usa: "email", "popup" (notificación). NO tiene "alarm".
                // Nosotros usamos "alarm" internamente para nuestras alarmas locales de Android.
                updateState { copy(reminders = reminders + event.reminder) }
            }

            is TaskEvent.OnRemoveReminder -> {
                val toRemove = current.reminders.find { it.method == event.reminder.method && it.minutes == event.reminder.minutes }
                if (toRemove != null) updateState { copy(reminders = reminders - toRemove) }
            }

            is TaskEvent.OnSaveClicked -> saveTask()
            is TaskEvent.OnCancelClicked -> _effect.value = TaskEffect.NavigateBack
            else -> {}
        }
    }

    private fun loadTaskFromId(taskId: Int) { /* TODO */ }

    private fun loadTask(task: TaskDomain) {
        val startCal = googleDateToCalendar(task.start)
        val endCal = googleDateToCalendar(task.end)
        val rruleString = task.recurrence.firstOrNull() ?: ""
        val daysCount = task.synkronRecurrenceDays.size

        // LÓGICA PARA RECUPERAR EL TIPO CORRECTO
        val inferredRecurrenceType = when {
            rruleString.contains("FREQ=DAILY") -> RecurrenceType.DAILY

            // Si es semanal, miramos cuántos días tiene
            rruleString.contains("FREQ=WEEKLY") -> {
                if (daysCount > 1) {
                    // Si hay más de un día (ej: Lunes y Jueves), fijo es Personalizado
                    RecurrenceType.CUSTOM
                } else {
                    // Si es solo 1 día, asumimos Semanalmente (o Personalizado, da igual visualmente)
                    RecurrenceType.WEEKLY
                }
            }
            // Fallback para datos antiguos
            daysCount == 7 -> RecurrenceType.DAILY
            daysCount > 0 -> RecurrenceType.CUSTOM
            else -> RecurrenceType.NONE
        }

        // CORRECCIÓN: Mapear RecurrenceType correctamente desde algún lado si lo guardas
        // Si no lo guardas explícitamente en BD, aquí tendrás que deducirlo o dejarlo por defecto.
        // Asumo que 'synkronRecurrenceDays' tiene los días custom.

        updateState {
            copy(
                id = task.id,
                title = task.summary,
                description = task.description ?: "",
                location = task.location ?: "",
                category = task.typeTask, // OJO: aquí va el ID de categoría ("work")
                colorId = task.colorId,
                priority = task.priority,
                selectedDate = startCal,
                startTime = startCal,
                endTime = endCal,
                isAllDay = task.start?.date != null, // Si tiene fecha (YYYY-MM-DD) y no datetime, es todo el día
                isNoDate = task.start == null,
                subTasks = task.subTasks,
                reminders = task.reminders.overrides,
                selectedRecurrenceDays = task.synkronRecurrenceDays.toSet(),
                recurrenceType = inferredRecurrenceType
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
                // Validación "Todo el día" único
                if (s.isAllDay) {
                    val dateToCheck = java.time.LocalDateTime.ofInstant(s.selectedDate.toInstant(), ZoneId.systemDefault()).toLocalDate()
                    // Asegúrate de que el repositorio tenga este método implementado
                    val exists = taskRepository.hasAllDayTaskOnDate(dateToCheck, s.id)
                    if (exists) {
                        updateState { copy(isLoading = false) }
                        _effect.value = TaskEffect.ShowMessage("⚠️ Ya tienes una tarea de 'Todo el día' para esta fecha.")
                        return@launch
                    }
                }

                // Generar fechas Google
                val (googleStart, googleEnd) = when {
                    s.isNoDate -> Pair(null, null)
                    s.isAllDay -> {
                        // Para todo el día, Google pide YYYY-MM-DD
                        val start = calendarToGoogleDateAllDay(s.selectedDate)
                        val end = calendarToGoogleDateAllDay(s.selectedDate) // Mismo día
                        Pair(start, end)
                    }
                    else -> {
                        // Para horas concretas
                        val start = calendarToGoogleDateTimed(s.startTime)
                        val end = calendarToGoogleDateTimed(s.endTime)
                        Pair(start, end)
                    }
                }

                val (finalDays, finalRrule) = when (s.recurrenceType) {

                    // CASO 1: "Todos los días"
                    RecurrenceType.DAILY -> {
                        Pair(
                            listOf(1, 2, 3, 4, 5, 6, 7), // Pintamos todo
                            listOf("RRULE:FREQ=DAILY")   // Regla diaria
                        )
                    }

                    // CASO 2: "Semanalmente" (Opción simple del menú)
                    // Significa: "Repetir cada semana el mismo día que hoy"
                    RecurrenceType.WEEKLY -> {
                        val currentDay = getSynkronDayFromCalendar(s.selectedDate)
                        val rrule = "RRULE:FREQ=WEEKLY;BYDAY=${dayIntToRRule(currentDay)}"

                        Pair(
                            listOf(currentDay), // Guardamos solo este día
                            listOf(rrule)
                        )
                    }

                    // CASO 3: "Personalizado" (Tus botones L, M, X...)
                    // Aquí SÍ hacemos caso a lo que el usuario marcó en los circulitos
                    RecurrenceType.CUSTOM -> {
                        // Si el usuario eligió "Personalizado" pero no marcó nada,
                        // por seguridad usamos el día actual (fallback).
                        var daysToSave = s.selectedRecurrenceDays.toList().sorted()
                        if (daysToSave.isEmpty()) {
                            daysToSave = listOf(getSynkronDayFromCalendar(s.selectedDate))
                        }

                        val byDay = daysToSave.joinToString(",") { dayIntToRRule(it) }
                        val rrule = "RRULE:FREQ=WEEKLY;BYDAY=$byDay" // Google lo ve como semanal con días específicos

                        Pair(
                            daysToSave,
                            listOf(rrule)
                        )
                    }

                    // CASO 4: "No se repite" (NONE) o cualquier otro
                    else -> Pair(emptyList(), emptyList())
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
                    synkronRecurrenceDays = finalDays,
                    recurrence = finalRrule,
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

    // Convierte 1->MO, 2->TU, ..., 7->SU
    private fun dayIntToRRule(day: Int): String {
        return when (day) {
            1 -> "MO" // Lunes
            2 -> "TU"
            3 -> "WE"
            4 -> "TH"
            5 -> "FR"
            6 -> "SA"
            7 -> "SU" // Domingo
            else -> ""
        }
    }

    // --- Helpers ---
    private fun calendarToGoogleDateTimed(cal: Calendar): GoogleEventDateTime {
        return GoogleEventDateTime(dateTime = cal.timeInMillis, timeZone = TimeZone.getDefault().id)
    }

    private fun calendarToGoogleDateAllDay(cal: Calendar): GoogleEventDateTime {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return GoogleEventDateTime(date = format.format(cal.time), timeZone = TimeZone.getDefault().id)
    }

    private fun googleDateToCalendar(gDate: GoogleEventDateTime?): Calendar {
        val cal = Calendar.getInstance()
        if (gDate != null) {
            if (gDate.dateTime != null) {
                cal.timeInMillis = gDate.dateTime!!
            } else if (!gDate.date.isNullOrEmpty()) {
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = format.parse(gDate.date!!)
                    if (date != null) cal.time = date
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        return cal
    }
    // Convierte el día de la semana de Android (donde Domingo=1)
    // al formato de Synkrón (donde Lunes=1, Martes=2, ..., Domingo=7)
    private fun getSynkronDayFromCalendar(cal: Calendar): Int {
        val day = cal.get(Calendar.DAY_OF_WEEK)
        return when (day) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1 // Fallback por defecto (Lunes) si pasa algo raro
        }
    }

    private fun updateState(update: TaskState.() -> TaskState) {
        _state.value = _state.value?.update()
    }
}