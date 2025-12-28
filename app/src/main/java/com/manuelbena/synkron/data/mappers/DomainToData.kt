package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.TaskDomain
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

fun TaskDomain.toEntity(): TaskEntity {

    // 1. Analizar Fecha de Inicio
    // Devuelve date=0 si es null (Sin Fecha)
    // Devuelve hour=-1 si es Todo el Día
    val (dateMillis, hourMinutes, timeZoneStr) = parseGoogleEventDateTime(this.start)

    // 2. Calcular Duración
    // Si date es 0 (sin fecha), la duración es 0.
    // Si es todo el día (hour -1), la duración es 0.
    val isNoDate = (dateMillis == 0L)
    val isAllDay = (hourMinutes == -1)

    val finalDuration = if (isNoDate) 0 else calculateDuration(this.start, this.end, isAllDay)

    // 3. Mapeo plano de listas
    val attendeesEmails = this.attendees.map { it.email }
    val reminderMinutes = this.reminders.overrides.map { it.minutes }
    val recurrenceRule = this.recurrence.firstOrNull()

    return TaskEntity(
        id = this.id,
        date = dateMillis,
        hour = hourMinutes,
        timeZone = timeZoneStr,
        duration = finalDuration,
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
        parentId = parentId,
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned,
        synkronRecurrence = this.synkronRecurrence,
        synkronRecurrenceDays = this.synkronRecurrenceDays
    )
}

// --- LÓGICA CORREGIDA ---

private fun parseGoogleEventDateTime(dateTime: GoogleEventDateTime?): Triple<Long, Int, String> {
    // CASO CRÍTICO: SI ES NULL, DEVOLVEMOS 0 EN LA FECHA
    if (dateTime == null) {
        return Triple(0L, 0, TimeZone.getDefault().id)
    }

    return try {
        val zoneIdStr = if (dateTime.timeZone.isNotEmpty()) dateTime.timeZone else TimeZone.getDefault().id
        val zoneId = ZoneId.of(zoneIdStr)

        when {
            // [PRIORIDAD 1] TODO EL DÍA (Tiene fecha "yyyy-MM-dd")
            !dateTime.date.isNullOrEmpty() -> {
                val localDate = LocalDate.parse(dateTime.date)
                val millis = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                Triple(millis, -1, zoneIdStr)
            }

            // [PRIORIDAD 2] HORA EXACTA (Tiene timestamp)
            dateTime.dateTime != null -> {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneIdStr))
                calendar.timeInMillis = dateTime.dateTime
                val hourMinutes = (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE)
                Triple(dateTime.dateTime, hourMinutes, zoneIdStr)
            }

            // Fallback (pero con fecha válida para no romper, o 0 si prefieres)
            else -> Triple(System.currentTimeMillis(), 0, zoneIdStr)
        }
    } catch (e: Exception) {
        Triple(System.currentTimeMillis(), 0, TimeZone.getDefault().id)
    }
}

private fun calculateDuration(start: GoogleEventDateTime?, end: GoogleEventDateTime?, isAllDay: Boolean): Int {
    if (isAllDay) return 0

    val startTime = start?.dateTime
    val endTime = end?.dateTime

    if (startTime == null) return 0

    // [REGLA DE 1 HORA] Si no hay fin, o el fin es anterior/igual al inicio -> 60 min
    if (endTime == null || endTime <= startTime) {
        return 60
    }

    return try {
        val diff = endTime - startTime
        val minutes = (diff / 60000).toInt()
        if (minutes > 0) minutes else 60
    } catch (e: Exception) {
        60
    }
}