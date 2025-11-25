package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventAttendee
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.TaskDomain
import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone

fun TaskEntity.toDomain(): TaskDomain {

    // 1. Reconstruir el objeto 'start' (TU LÓGICA ORIGINAL)
    val startDateTime = createGoogleEventDateTime(this.date, this.hour, this.timeZone)

    // 2. Reconstruir el objeto 'end' (TU LÓGICA ORIGINAL)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@toDomain.date
        set(Calendar.HOUR_OF_DAY, this@toDomain.hour / 60)
        set(Calendar.MINUTE, this@toDomain.hour % 60)
    }
    val endTimeMillis = calendar.timeInMillis + (this.duration * 60 * 1000)
    val endDateTime = createGoogleEventDateTime(endTimeMillis, this.timeZone)

    // 3. Reconstruir 'attendees' (TU LÓGICA ORIGINAL)
    val attendees = this.attendeesEmails.map { email ->
        GoogleEventAttendee(email = email, responseStatus = "needsAction")
    }

    // 4. Reconstruir 'reminders' (TU LÓGICA ORIGINAL)
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
        categoryIcon = this.categoryIcon,
        categoryColor = this.categoryColor,
        subTasks = this.subTasks,
        typeTask = this.typeTask,
        priority = this.priority,
        isActive = this.isActive,
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned,
        // NUEVOS MAPEOS
        synkronRecurrence = this.synkronRecurrence,
        synkronRecurrenceDays = this.synkronRecurrenceDays
    )
}

// ... (MANTÉN TUS FUNCIONES PRIVADAS createGoogleEventDateTime EXACTAMENTE COMO ESTABAN) ...
private fun createGoogleEventDateTime(dateMillis: Long, timeZone: String): GoogleEventDateTime {
    val instant = Instant.ofEpochMilli(dateMillis)
    val zoneId = TimeZone.getTimeZone(timeZone).toZoneId()
    val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)
    return GoogleEventDateTime(dateTime = zonedDateTime, timeZone = timeZone)
}

private fun createGoogleEventDateTime(dateMillis: Long, hourMinutes: Int, timeZone: String): GoogleEventDateTime {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hourMinutes / 60)
        set(Calendar.MINUTE, hourMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val instant = calendar.toInstant()
    val zoneId = TimeZone.getTimeZone(timeZone).toZoneId()
    val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)
    return GoogleEventDateTime(dateTime = zonedDateTime, timeZone = timeZone)
}