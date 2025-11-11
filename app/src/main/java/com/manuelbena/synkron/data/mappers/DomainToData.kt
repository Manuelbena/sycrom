package com.manuelbena.synkron.data.mappers

import TaskDomain
import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventDateTime

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Convierte un [TaskDomain] (anidado, de UI/Dominio) a un [TaskEntity] (plano, de BBDD).
 */
fun TaskDomain.toEntity(): TaskEntity {

    // 1. Aplanar 'start'
    val (date, hour, timeZone) = parseGoogleEventDateTime(this.start)

    // 2. Aplanar 'end' y calcular duración
    val duration = calculateDuration(this.start, this.end)

    // 3. Aplanar 'attendees'
    val attendeesEmails = this.attendees.map { it.email }

    // 4. Aplanar 'reminders'
    val reminderMinutes = this.reminders.overrides.map { it.minutes }

    // 5. Aplanar 'recurrence'
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
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned
    )
}

// --- Helpers de Mapeo ---

private val isoFormatter by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
}

private fun parseGoogleEventDateTime(dateTime: GoogleEventDateTime?): Triple<Long, Int, String> {
    if (dateTime == null || dateTime.dateTime.isEmpty()) {
        val now = Calendar.getInstance()
        return Triple(now.timeInMillis, 0, TimeZone.getDefault().id)
    }

    return try {
        val tz = TimeZone.getTimeZone(dateTime.timeZone)
        isoFormatter.timeZone = tz
        val date = isoFormatter.parse(dateTime.dateTime)
        if (date != null) {
            val calendar = Calendar.getInstance(tz).apply { time = date }
            val dateMillis = calendar.timeInMillis
            val hour = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            Triple(dateMillis, hour, dateTime.timeZone)
        } else {
            Triple(0L, 0, dateTime.timeZone)
        }
    } catch (e: Exception) {
        Triple(0L, 0, TimeZone.getDefault().id)
    }
}

private fun calculateDuration(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Int {
    if (start == null || end == null) return 0
    return try {
        val startDate = isoFormatter.parse(start.dateTime)
        val endDate = isoFormatter.parse(end.dateTime)
        if (startDate != null && endDate != null) {
            val diffMillis = endDate.time - startDate.time
            (diffMillis / (1000 * 60)).toInt()
        } else 0
    } catch (e: Exception) { 0 }
}