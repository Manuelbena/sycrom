package com.manuelbena.synkron.presentation.task

import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import java.util.Calendar

data class TaskState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val location: String = "",

    // Nueva lógica de Tiempo
    val selectedDate: Calendar = Calendar.getInstance(), // La fecha base (día)
    val startTime: Calendar = Calendar.getInstance(),    // Hora inicio
    val endTime: Calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }, // Hora fin (+1h por defecto)
    val isAllDay: Boolean = false,
    val isNoDate: Boolean = false,

    // Lógica de Negocio
    val category: String = "Personal",
    val colorId: String = "2",
    val priority: String = "Media",

    // Listas Dinámicas
    val subTasks: List<SubTaskDomain> = emptyList(),
    val reminders: List<GoogleEventReminder> = emptyList(), // Nueva lista de recordatorios

    // Recurrencia
    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val selectedRecurrenceDays: Set<Int> = emptySet(), // Para los chips de L, M, X... (Calendar.MONDAY, etc)

    val error: String? = null,
    val isSaved: Boolean = false
)

enum class RecurrenceType { NONE, DAILY, WEEKLY, CUSTOM }

sealed class TaskEvent {
    // Inputs básicos
    data class OnTitleChange(val title: String) : TaskEvent()
    data class OnDescriptionChange(val desc: String) : TaskEvent()
    data class OnLocationChange(val loc: String) : TaskEvent()

    // Tabs (Schedule, AllDay, NoDate)
    data class OnTaskTypeChanged(val tabIndex: Int) : TaskEvent()

    // Fechas y Horas
    data class OnDateSelected(val date: Long) : TaskEvent()
    data class OnStartTimeSelected(val hour: Int, val minute: Int) : TaskEvent()
    data class OnEndTimeSelected(val hour: Int, val minute: Int) : TaskEvent()

    // Listas
    data class OnAddSubTask(val text: String) : TaskEvent()
    data class OnRemoveSubTask(val item: SubTaskDomain) : TaskEvent()
    object OnAddReminderClicked : TaskEvent() // Abre diálogo
    data class OnAddReminder(val reminder: GoogleEventReminder) : TaskEvent()
    data class OnRemoveReminder(val reminder: GoogleEventReminder) : TaskEvent()

    // Otros
    data class OnCategorySelected(val category: String, val colorId: String) : TaskEvent()
    data class OnPrioritySelected(val priority: String) : TaskEvent()

    // Recurrencia
    object OnRecurrenceSelectorClicked : TaskEvent()
    data class OnRecurrenceDayToggled(val day: Int, val isSelected: Boolean) : TaskEvent() // Click en chips L, M...

    // Acciones
    object OnSaveClicked : TaskEvent()
    object OnCancelClicked : TaskEvent()
}

sealed class TaskEffect {
    object NavigateBack : TaskEffect()
    data class ShowMessage(val msg: String) : TaskEffect()
    object ShowRecurrenceDialog : TaskEffect() // Efecto para abrir tu diálogo de selección
    object ShowReminderDialog : TaskEffect()
}