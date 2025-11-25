package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.TaskDomain
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone

fun TaskDomain.toEntity(): TaskEntity {

    // 1. Aplanar 'start' (TU LÓGICA ORIGINAL)
    val (date, hour, timeZone) = parseGoogleEventDateTime(this.start)

    // 2. Aplanar 'end' y calcular duración (TU LÓGICA ORIGINAL)
    val duration = calculateDuration(this.start, this.end)

    // 3. Aplanar 'attendees' (TU LÓGICA ORIGINAL)
    val attendeesEmails = this.attendees.map { it.email }

    // 4. Aplanar 'reminders' (TU LÓGICA ORIGINAL)
    val reminderMinutes = this.reminders.overrides.map { it.minutes }

    // 5. Aplanar 'recurrence' (TU LÓGICA ORIGINAL)
    val recurrenceRule = this.recurrence.firstOrNull()

    return TaskEntity(
        id = this.id,
        date = date,
        hour = hour,
        timeZone = timeZone,
        duration = duration,
        summary = this.summary,
        description = this.description,
        location = this.location,
        colorId = this.colorId,
        typeTask = this.typeTask,
        priority = this.priority,
        attendeesEmails = attendeesEmails,
        recurrenceRule = recurrenceRule,
        reminderMinutes = reminderMinutes,
        transparency = this.transparency,
        conferenceLink = this.conferenceLink,
        subTasks = this.subTasks,
        isActive = this.isActive,
        categoryIcon = this.categoryIcon,
        categoryColor = this.categoryColor,
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned,
        // NUEVOS MAPEOS
        synkronRecurrence = this.synkronRecurrence,
        synkronRecurrenceDays = this.synkronRecurrenceDays
    )
}

// ... (MANTÉN TUS FUNCIONES PRIVADAS parseGoogleEventDateTime y calculateDuration EXACTAMENTE COMO ESTABAN) ...
private fun parseGoogleEventDateTime(dateTime: GoogleEventDateTime?): Triple<Long, Int, String> {
    if (dateTime?.dateTime == null) {
        val now = Calendar.getInstance()
        return Triple(now.timeInMillis, 0, TimeZone.getDefault().id)
    }
    return try {
        val zdt: ZonedDateTime = dateTime.dateTime
        val dateMillis = zdt.toInstant().toEpochMilli()
        val hour = zdt.hour * 60 + zdt.minute
        val timeZone = zdt.zone.id
        Triple(dateMillis, hour, timeZone)
    } catch (e: Exception) {
        Triple(0L, 0, TimeZone.getDefault().id)
    }
}

private fun calculateDuration(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Int {
    if (start?.dateTime == null || end?.dateTime == null) return 0
    return try {
        val duration = Duration.between(start.dateTime, end.dateTime)
        duration.toMinutes().toInt()
    } catch (e: Exception) { 0 }
}