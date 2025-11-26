package com.manuelbena.synkron.presentation.task

import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import java.util.Calendar

data class TaskState(
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val location: String = "",

    val selectedDate: Calendar = Calendar.getInstance(),
    val startTime: Calendar = Calendar.getInstance(),
    val endTime: Calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) },
    val isAllDay: Boolean = false,
    val isNoDate: Boolean = false,

    val category: String = "Personal",
    val colorId: String = "2", // Sage por defecto
    val priority: String = "Media",

    val subTasks: List<SubTaskDomain> = emptyList(),
    val reminders: List<GoogleEventReminder> = emptyList(),

    val recurrenceType: RecurrenceType = RecurrenceType.NONE,
    val selectedRecurrenceDays: Set<Int> = emptySet(),

    val error: String? = null,
    val isSaved: Boolean = false
)

enum class RecurrenceType { NONE, DAILY, WEEKLY, CUSTOM }

sealed class TaskEvent {
    data class OnTitleChange(val title: String) : TaskEvent()
    data class OnDescriptionChange(val desc: String) : TaskEvent()
    data class OnLocationChange(val loc: String) : TaskEvent()
    data class OnTaskTypeChanged(val tabIndex: Int) : TaskEvent()

    data class OnDateSelected(val date: Long) : TaskEvent()
    data class OnStartTimeSelected(val hour: Int, val minute: Int) : TaskEvent()
    data class OnEndTimeSelected(val hour: Int, val minute: Int) : TaskEvent()

    data class OnAddSubTask(val text: String) : TaskEvent()
    data class OnRemoveSubTask(val item: SubTaskDomain) : TaskEvent()
    data class OnReorderSubTasks(val items: List<SubTaskDomain>) : TaskEvent() // Nuevo para drag & drop
    // Reemplaza los eventos individuales si quieres simplificar, o añade este:
    data class OnUpdateReminders(val reminders: List<GoogleEventReminder>) : TaskEvent()

    object OnAddReminderClicked : TaskEvent()
    data class OnAddReminder(val reminder: GoogleEventReminder) : TaskEvent()
    data class OnRemoveReminder(val reminder: GoogleEventReminder) : TaskEvent() // Si necesitas borrar

    // --- EVENTO QUE FALTABA ---
    object OnCategorySelectorClicked : TaskEvent()
    data class OnCategorySelected(val category: String, val colorId: String) : TaskEvent()

    data class OnPrioritySelected(val priority: String) : TaskEvent()

    object OnRecurrenceSelectorClicked : TaskEvent()
    data class OnRecurrenceTypeSelected(val type: RecurrenceType) : TaskEvent()
    data class OnRecurrenceDayToggled(val day: Int, val isSelected: Boolean) : TaskEvent()

    object OnSaveClicked : TaskEvent()
    object OnCancelClicked : TaskEvent()
}

sealed class TaskEffect {
    object NavigateBack : TaskEffect()
    data class ShowMessage(val msg: String) : TaskEffect()
    object ShowCategoryDialog : TaskEffect() // Nuevo
    object ShowRecurrenceDialog : TaskEffect()
    object ShowReminderDialog : TaskEffect()
}