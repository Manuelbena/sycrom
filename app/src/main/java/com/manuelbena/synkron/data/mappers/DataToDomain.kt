package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventAttendee
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.TaskDomain

// --- IMPORTACIONES AÑADIDAS ---
import java.time.Instant
import java.time.ZonedDateTime
// --- FIN DE IMPORTACIONES AÑADIDAS ---

import java.util.Calendar
import java.util.TimeZone

// --- ¡ELIMINADO! ---
// private val isoFormatter by lazy { ... }
// Ya no es necesario y es la fuente del error.

/**
 * Convierte un [TaskEntity] (plano, de BBDD) a un [TaskDomain] (anidado, de UI/Dominio).
 */
fun TaskEntity.toDomain(): TaskDomain {

    // 1. Reconstruir el objeto 'start'
    val startDateTime = createGoogleEventDateTime(this.date, this.hour, this.timeZone)

    // 2. Reconstruir el objeto 'end' (calculado)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@toDomain.date
        set(Calendar.HOUR_OF_DAY, this@toDomain.hour / 60)
        set(Calendar.MINUTE, this@toDomain.hour % 60)
    }
    val endTimeMillis = calendar.timeInMillis + (this.duration * 60 * 1000)
    val endDateTime = createGoogleEventDateTime(endTimeMillis, this.timeZone)

    // 3. Reconstruir 'attendees'
    val attendees = this.attendeesEmails.map { email ->
        GoogleEventAttendee(email = email, responseStatus = "needsAction")
    }



    return TaskDomain(
        id = this.id,
        summary = this.summary,
        description = this.description,
        location = this.location,
        colorId = this.colorId,
        start = startDateTime,
        end = endDateTime,
        attendees = attendees,
        recurrence = if (this.recurrenceRule != null) listOf(this.recurrenceRule) else emptyList(),
        transparency = this.transparency,
        conferenceLink = this.conferenceLink,
        subTasks = this.subTasks,
        typeTask = this.typeTask,
        categoryIcon = this.categoryIcon,
        categoryColor = this.categoryColor,
        priority = this.priority,
        isActive = this.isActive,
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned,
        reminders = this.reminders ?: GoogleEventReminders(false, emptyList()), // Manejo de nulos seguro
    )
}

// --- Helpers de Mapeo (REESCRITOS) ---

/**
 * Helper REESCRITO para crear un ZonedDateTime a partir de milisegundos.
 */
private fun createGoogleEventDateTime(dateMillis: Long, timeZone: String): GoogleEventDateTime {
    val instant = Instant.ofEpochMilli(dateMillis)
    val zoneId = TimeZone.getTimeZone(timeZone).toZoneId()
    val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)

    return GoogleEventDateTime(
        dateTime = zonedDateTime, // Ahora pasamos el objeto ZonedDateTime
        timeZone = timeZone
    )
}

/**
 * Helper REESCRITO para crear un ZonedDateTime a partir de fecha + minutos.
 */
private fun createGoogleEventDateTime(dateMillis: Long, hourMinutes: Int, timeZone: String): GoogleEventDateTime {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hourMinutes / 60)
        set(Calendar.MINUTE, hourMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Convertimos el Calendar a ZonedDateTime
    val instant = calendar.toInstant()
    val zoneId = TimeZone.getTimeZone(timeZone).toZoneId()
    val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)

    return GoogleEventDateTime(
        dateTime = zonedDateTime, // Ahora pasamos el objeto ZonedDateTime
        timeZone = timeZone
    )
}