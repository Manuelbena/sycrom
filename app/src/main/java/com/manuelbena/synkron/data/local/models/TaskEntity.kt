package com.manuelbena.synkron.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manuelbena.synkron.domain.models.SubTaskDomain

/**
 * Representa la tabla 'task_table' en la base de datos Room.
 * Es una representación "plana" de una tarea, optimizada para almacenamiento local.
 * La lógica de "Mappers" la convertirá a/desde TaskDomain.
 */
@Entity(tableName = "task_table")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Campos de Fecha/Hora (planos)
    val date: Long,           // -> Mapea a/desde TaskDomain.start
    val hour: Int,            // -> Mapea a/desde TaskDomain.start
    val timeZone: String,     // -> Mapea a/desde TaskDomain.start.timeZone
    val duration: Int,        // -> Se usa para calcular TaskDomain.end
    val categoryIcon: String,  // Guardamos "ic_work"
    val categoryColor: String, // Guardamos "category_work"

    // Campos de Texto (planos)
    val summary: String,      // -> Mapea a/desde TaskDomain.summary
    val description: String?, // -> Mapea a/desde TaskDomain.description
    val location: String?,    // -> Mapea a/desde TaskDomain.location
    val colorId: String?,     // -> Mapea a/desde TaskDomain.colorId
    val typeTask: String,     // -> Mapea a/desde TaskDomain.typeTask
    val priority: String,     // -> Mapea a/desde TaskDomain.priority

    // Campos de Google Calendar (planos, convertidos)
    val attendeesEmails: List<String>,    // -> Mapea a/desde TaskDomain.attendees
    val recurrenceRule: String?,        // -> Mapea a/desde TaskDomain.recurrence
    val reminderMinutes: List<Int>,     // -> Mapea a/desde TaskDomain.reminders
    val transparency: String,           // -> Mapea a/desde TaskDomain.transparency
    val conferenceLink: String?,        // -> Mapea a/desde TaskDomain.conferenceLink

    // Campos de Estado (planos)
    val subTasks: List<SubTaskDomain>,
    val isActive: Boolean,
    val isDone: Boolean,
    val isDeleted: Boolean,
    val isArchived: Boolean,
    val isPinned: Boolean
)