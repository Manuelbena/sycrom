package com.manuelbena.synkron.presentation.task

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.manuelbena.synkron.base.BaseViewModel
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.usecase.InsertNewTaskUseCase
import com.manuelbena.synkron.domain.usecase.UpdateTaskUseCase
import com.manuelbena.synkron.presentation.util.SingleLiveEvent
import com.manuelbena.synkron.presentation.util.toGoogleEventDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val insertTaskUseCase: InsertNewTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase
) : BaseViewModel<TaskEvent>() {

    private val _state = MutableLiveData(TaskState())
    val state: LiveData<TaskState> = _state

    private val _effect = SingleLiveEvent<TaskEffect>()
    val effect: LiveData<TaskEffect> = _effect

    fun onEvent(event: TaskEvent) {
        val current = _state.value ?: TaskState()

        when (event) {
            // ... (Inputs de texto igual que antes) ...
            is TaskEvent.OnTitleChange -> updateState { copy(title = event.title) }
            is TaskEvent.OnDescriptionChange -> updateState { copy(description = event.desc) }
            is TaskEvent.OnLocationChange -> updateState { copy(location = event.loc) }

            // ... (Tabs y Fechas igual que antes) ...
            is TaskEvent.OnTaskTypeChanged -> updateState { copy(isAllDay = event.tabIndex == 1, isNoDate = event.tabIndex == 2) }
            is TaskEvent.OnDateSelected -> {
                val newCal = Calendar.getInstance().apply { timeInMillis = event.date }
                newCal.set(Calendar.HOUR_OF_DAY, current.selectedDate.get(Calendar.HOUR_OF_DAY))
                newCal.set(Calendar.MINUTE, current.selectedDate.get(Calendar.MINUTE))
                updateState { copy(selectedDate = newCal) }
            }
            is TaskEvent.OnStartTimeSelected -> {
                val newStart = (current.startTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Lógica de empuje (Inicio > Fin)
                var newEnd = current.endTime
                if (newStart.after(newEnd)) {
                    newEnd = (newStart.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 1) }
                }

                // --- NUEVA LÓGICA: Límite 23:59 ---
                // Calculamos el final del día de la nueva hora de inicio
                val endOfDay = (newStart.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // Si la hora fin calculada se pasa al día siguiente o supera las 23:59
                if (newEnd.after(endOfDay) || newEnd.get(Calendar.DAY_OF_YEAR) != newStart.get(Calendar.DAY_OF_YEAR)) {
                    newEnd = endOfDay // La topeamos a las 23:59
                }

                updateState { copy(startTime = newStart, endTime = newEnd) }
            }
            is TaskEvent.OnLoadTaskForEdit -> {
                loadTaskForEdit(event.task)
            }

            is TaskEvent.OnEndTimeSelected -> {
                val newEnd = (current.endTime.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, event.hour)
                    set(Calendar.MINUTE, event.minute)
                }

                // VALIDACIÓN: La hora fin no puede ser anterior al inicio
                if (newEnd.before(current.startTime)) {
                    _effect.value = TaskEffect.ShowMessage("La hora fin no puede ser anterior al inicio")
                } else {
                    // Como seleccionamos hora/minuto sobre el mismo objeto Calendar del día,
                    // implícitamente estamos en el mismo día.
                    // Solo aseguramos que no haya saltos raros si la lógica cambiara.
                    updateState { copy(endTime = newEnd) }
                }
            }

            is TaskEvent.OnAddSubTask -> {
                if (event.text.isNotBlank()) {
                    val sub = SubTaskDomain(id = UUID.randomUUID().toString(), title = event.text, isDone = false)
                    updateState { copy(subTasks = subTasks + sub) }
                }
            }
            is TaskEvent.OnRemoveSubTask -> {
                updateState { copy(subTasks = subTasks - event.item) }
            }
            is TaskEvent.OnReorderSubTasks -> {
                updateState { copy(subTasks = event.items) }
            }
            is TaskEvent.OnAddReminderClicked -> _effect.value = TaskEffect.ShowReminderDialog
            is TaskEvent.OnAddReminder -> updateState { copy(reminders = reminders + event.reminder) }
            is TaskEvent.OnRemoveReminder -> updateState { copy(reminders = reminders - event.reminder) }
            is TaskEvent.OnCategorySelectorClicked -> _effect.value = TaskEffect.ShowCategoryDialog
            is TaskEvent.OnCategorySelected -> updateState { copy(category = event.category, colorId = event.colorId) }
            is TaskEvent.OnPrioritySelected -> updateState { copy(priority = event.priority) }
            is TaskEvent.OnRecurrenceSelectorClicked -> _effect.value = TaskEffect.ShowRecurrenceDialog
            is TaskEvent.OnRecurrenceTypeSelected -> updateState { copy(recurrenceType = event.type) }
            is TaskEvent.OnRecurrenceDayToggled -> {
                val newSet = current.selectedRecurrenceDays.toMutableSet()
                if (event.isSelected) newSet.add(event.day) else newSet.remove(event.day)
                updateState { copy(selectedRecurrenceDays = newSet) }
            }

            is TaskEvent.OnSaveClicked -> saveTask()
            is TaskEvent.OnCancelClicked -> _effect.value = TaskEffect.NavigateBack
            is TaskEvent.OnUpdateReminders -> updateState { copy(reminders = event.reminders) }
        }
    }
    private fun loadTaskForEdit(task: TaskDomain) {
        // 1. Convertimos las fechas con la nueva función compatible con ZonedDateTime
        val startCal = googleDateToCalendar(task.start)
        val endCal = googleDateToCalendar(task.end)

        // 2. DETECCIÓN DE TODO EL DÍA:
        // Como tu modelo ya no tiene el campo 'date' (String), no podemos distinguir
        // con seguridad si es "Todo el día" solo mirando si 'dateTime' es nulo.
        // Asumiremos Falso por defecto para evitar errores de compilación "Unresolved reference: date".
        // Si necesitas recuperar esa lógica, tu modelo GoogleEventDateTime debería tener un campo booleano o conservar el campo 'date'.
        val isAllDayEvent = false

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

                isAllDay = isAllDayEvent,
                isNoDate = false,

                subTasks = task.subTasks.toMutableList(),

                //recurrenceType = task.synkronRecurrence,
                selectedRecurrenceDays = task.synkronRecurrenceDays.toMutableSet(),

                reminders = task.reminders.overrides.map {
                    com.manuelbena.synkron.domain.models.GoogleEventReminder(
                        method = it.method,
                        minutes = it.minutes,
                        message = "Recordatorio"
                    )
                }
            )
        }
    }

    private fun saveTask() {
        val s = _state.value ?: return

        // 1. Validaciones básicas
        if (s.title.isBlank()) {
            _effect.value = TaskEffect.ShowMessage("Falta el título")
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            try {
                // 2. Preparar fechas
                val finalStart = (s.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, s.startTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, s.startTime.get(Calendar.MINUTE))
                }
                val finalEnd = (s.selectedDate.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, s.endTime.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, s.endTime.get(Calendar.MINUTE))
                }

                // 3. Crear el objeto de dominio
                val task = TaskDomain(
                    // IMPORTANTE: Pasamos el ID del estado.
                    // Si es 0 (nueva), Room lo autogenerará. Si tiene valor (edición), Room actualizará esa fila.
                    id = s.id,

                    typeTask = s.category,
                    summary = s.title,
                    description = s.description,
                    location = s.location,

                    // Fechas convertidas
                    start = if (s.isNoDate) null else finalStart.toGoogleEventDateTime(),
                    end = if (s.isNoDate) null else finalEnd.toGoogleEventDateTime(),

                    colorId = s.colorId,
                    priority = s.priority,
                    subTasks = s.subTasks,
                    reminders = GoogleEventReminders(overrides = s.reminders),

                    // Mapeo de Recurrencia (usando las funciones que creamos antes)
                    //synkronRecurrence = mapToDomainRecurrence(s.recurrenceType),
                    synkronRecurrenceDays = s.selectedRecurrenceDays.toList(),

                    // Valores por defecto
                    isActive = true,
                    isDone = false,
                    isDeleted = false,
                    isArchived = false,
                    isPinned = false,
                    transparency = "opaque"
                )

                // 4. LÓGICA DE DECISIÓN: ¿Insertar o Actualizar?
                if (s.id == 0) {
                    // Es nueva
                    insertTaskUseCase(task)
                    _effect.value = TaskEffect.ShowMessage("Tarea creada con éxito")
                } else {
                    // Ya existe (Edición)
                    updateTaskUseCase(task)
                    _effect.value = TaskEffect.ShowMessage("Tarea actualizada")
                }

                // 5. Salir
                _effect.value = TaskEffect.NavigateBack

            } catch (e: Exception) {
                updateState { copy(isLoading = false) }
                _effect.value = TaskEffect.ShowMessage("Error al guardar: ${e.message}")
                e.printStackTrace()
            }
        }
    }


    private fun googleDateToCalendar(gDate: com.manuelbena.synkron.domain.models.GoogleEventDateTime?): Calendar {
        val cal = Calendar.getInstance()
        // Si es nulo o no tiene dateTime, devolvemos el calendario con la hora actual
        if (gDate?.dateTime == null) return cal

        try {
            // CONVERSIÓN DIRECTA: ZonedDateTime -> Date -> Calendar
            // 'toInstant()' convierte la fecha/hora a un punto exacto en el tiempo (UTC)
            val instant = gDate.dateTime.toInstant()
            val date = Date.from(instant)
            cal.time = date
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cal
    }
    private fun updateState(update: TaskState.() -> TaskState) {
        _state.value = _state.value?.update()
    }
}