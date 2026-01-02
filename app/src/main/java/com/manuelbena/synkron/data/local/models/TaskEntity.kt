package com.manuelbena.synkron.data.local.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manuelbena.synkron.domain.models.NotificationType
import com.manuelbena.synkron.domain.models.SubTaskDomain

@Entity(tableName = "task_table")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val googleCalendarId: String? = null,

    // Campos de Fecha/Hora
    val date: Long,
    val hour: Int,
    val timeZone: String,
    val duration: Int,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,

    // Campos de Texto
    val summary: String,
    val description: String?,
    val location: String?,
    val colorId: String?,
    val typeTask: String,

    // --- ¡NUEVAS COLUMNAS! ---
    val categoryIcon: String = "",
    val categoryColor: String = "",
    // -------------------------

    val priority: String,

    // Campos de Google Calendar
    val attendeesEmails: List<String>,
    val recurrenceRule: String?,
    val reminderMinutes: List<Int>,
    val transparency: String,
    val conferenceLink: String?,

    // Campos de Estado
    val subTasks: List<SubTaskDomain>,
    val isActive: Boolean,
    val isDone: Boolean,
    val isDeleted: Boolean,
    val isArchived: Boolean,
    val isPinned: Boolean,

    // Recurrencia Interna
    val synkronRecurrence: NotificationType = NotificationType.NOTIFICATION,
    val synkronRecurrenceDays: List<Int> = emptyList()
)