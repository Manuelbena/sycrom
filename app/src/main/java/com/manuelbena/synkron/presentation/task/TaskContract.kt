package com.manuelbena.synkron.presentation.task

import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.domain.models.RecurrenceType
import java.util.Calendar

data class TaskState(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val location: String = "",

    val selectedDate: Calendar = Calendar.getInstance(),
    val startTime: Calendar = Calendar.getInstance(),
    val endTime: Calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) },

    val isAllDay: Boolean = false,
    val isNoDate: Boolean = false,

    val category: String = "Personal",
    val colorId: String? = null,
    val priority: String = "Media",

    val subTasks: List<SubTaskDomain> = emptyList(),
    val reminders: List<GoogleEventReminder> = emptyList(),

    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val selectedRecurrenceDays: Set<Int> = emptySet(),

    val isLoading: Boolean = false
)

sealed class TaskEvent {
    // Textos
    data class OnTitleChange(val title: String) : TaskEvent()
    data class OnDescriptionChange(val desc: String) : TaskEvent()
    data class OnLocationChange(val loc: String) : TaskEvent()

    // Subtareas
    data class OnAddSubTask(val text: String) : TaskEvent()
    data class OnRemoveSubTask(val item: SubTaskDomain) : TaskEvent()
    data class OnReorderSubTasks(val items: List<SubTaskDomain>) : TaskEvent()

    // Recordatorios
    object OnAddReminderClicked : TaskEvent()
    data class OnAddReminder(val reminder: GoogleEventReminder) : TaskEvent()
    data class OnRemoveReminder(val reminder: GoogleEventReminder) : TaskEvent()
    data class OnUpdateReminders(val reminders: List<GoogleEventReminder>) : TaskEvent()

    // Fechas y Horas
    object OnDateClicked : TaskEvent()
    data class OnDateSelected(val date: Long) : TaskEvent()
    object OnStartTimeClicked : TaskEvent()
    data class OnStartTimeSelected(val hour: Int, val minute: Int) : TaskEvent()
    object OnEndTimeClicked : TaskEvent()
    data class OnEndTimeSelected(val hour: Int, val minute: Int) : TaskEvent()

    data class OnTaskTypeChanged(val tabIndex: Int) : TaskEvent()

    // Selectores
    object OnCategorySelectorClicked : TaskEvent()
    data class OnCategorySelected(val category: String, val colorId: String?) : TaskEvent()

    object OnPrioritySelectorClicked : TaskEvent()
    data class OnPrioritySelected(val priority: String) : TaskEvent()

    object OnRecurrenceSelectorClicked : TaskEvent()
    data class OnRecurrenceTypeSelected(val type: RecurrenceType) : TaskEvent()
    data class OnRecurrenceDayToggled(val day: Int, val isSelected: Boolean) : TaskEvent()

    // Acciones
    data class OnLoadTaskForEdit(val task: TaskDomain) : TaskEvent()

    data class OnLoadTaskById(val taskId: Int) : TaskEvent() // Carga por ID (Lo que usa Navigation)
    object OnSaveClicked : TaskEvent()
    object OnCancelClicked : TaskEvent()
}

sealed class TaskEffect {
    object NavigateBack : TaskEffect()
    data class ShowMessage(val msg: String) : TaskEffect()

    data class ShowDatePicker(val date: Long) : TaskEffect()
    data class ShowTimePicker(val time: Calendar, val isStartTime: Boolean) : TaskEffect()

    object ShowCategoryDialog : TaskEffect()
    object ShowPriorityDialog : TaskEffect()
    object ShowReminderDialog : TaskEffect()
    object ShowRecurrenceDialog : TaskEffect()
}