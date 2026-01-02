package com.manuelbena.synkron.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TaskDomain(
    // --- Campos Internos de Synkrón ---
    val id: Int = 0,
    val googleCalendarId: String? = null,

    val subTasks: List<SubTaskDomain> = emptyList(),
    val typeTask : String, // Nombre de la categoría (ej: "Trabajo")
    val parentId: String? = null,



    val priority: String = "Media",
    val isActive: Boolean = true,
    val isDone: Boolean = false,
    val isDeleted: Boolean = false,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,

    // Nuevos campos de Recurrencia (Turno anterior)
    val synkronRecurrence: NotificationType = NotificationType.NOTIFICATION,
    val synkronRecurrenceDays: List<Int> = emptyList(),

    // --- Campos Mapeados 1:1 con Google Calendar ---
    val summary: String,
    val description: String? = null,
    val location: String? = null,
    val colorId: String? = null, // Color de Google (distinto de categoryColor)
    val start: GoogleEventDateTime?,
    val end: GoogleEventDateTime?,
    val attendees: List<GoogleEventAttendee> = emptyList(),
    val recurrence: List<String> = emptyList(),
    val reminders: GoogleEventReminders = GoogleEventReminders(useDefault = false),
    val transparency: String = "opaque",
    val conferenceLink: String? = null
) : Parcelable