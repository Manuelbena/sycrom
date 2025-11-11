package com.manuelbena.synkron.data.mappers

import TaskDomain
import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventAttendee
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.GoogleEventReminders

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val isoFormatter by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
}

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

    // 4. Reconstruir 'reminders'
    val reminders = GoogleEventReminders(
        useDefault = false,
        overrides = this.reminderMinutes.map { minutes ->
            GoogleEventReminder(method = "popup", minutes = minutes)
        }
    )

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
        reminders = reminders,
        transparency = this.transparency,
        conferenceLink = this.conferenceLink,
        subTasks = this.subTasks,
        typeTask = this.typeTask,
        priority = this.priority,
        isActive = this.isActive,
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned
    )
}

// --- Helpers de Mapeo ---

private fun createGoogleEventDateTime(dateMillis: Long, timeZone: String): GoogleEventDateTime {
    val date = Date(dateMillis)
    val tz = TimeZone.getTimeZone(timeZone)
    isoFormatter.timeZone = tz
    return GoogleEventDateTime(
        dateTime = isoFormatter.format(date),
        timeZone = timeZone
    )
}

private fun createGoogleEventDateTime(dateMillis: Long, hourMinutes: Int, timeZone: String): GoogleEventDateTime {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hourMinutes / 60)
        set(Calendar.MINUTE, hourMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val tz = TimeZone.getTimeZone(timeZone)
    isoFormatter.timeZone = tz
    return GoogleEventDateTime(
        dateTime = isoFormatter.format(calendar.time),
        timeZone = timeZone
    )
}