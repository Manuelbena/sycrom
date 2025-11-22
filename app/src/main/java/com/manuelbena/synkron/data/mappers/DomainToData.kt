package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.TaskDomain

// --- IMPORTACIONES ELIMINADAS ---
// import java.text.SimpleDateFormat

// --- IMPORTACIONES AÑADIDAS ---
import java.time.Duration
import java.time.ZonedDateTime
// --- FIN IMPORTACIONES ---

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
        categoryIcon = this.categoryIcon,
        categoryColor = this.categoryColor,
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

// --- Helpers de Mapeo (REESCRITOS) ---

// --- ¡ELIMINADO! ---
// private val isoFormatter by lazy { ... }

/**
 * Helper REESCRITO para leer desde ZonedDateTime.
 */
private fun parseGoogleEventDateTime(dateTime: GoogleEventDateTime?): Triple<Long, Int, String> {
    // Usamos el elvis operator (?.), la comprobación es más limpia.
    // Si dateTime o dateTime.dateTime es null, usamos los valores por defecto.
    if (dateTime?.dateTime == null) {
        val now = Calendar.getInstance()
        // Devolvemos los valores por defecto que tenías.
        return Triple(now.timeInMillis, 0, TimeZone.getDefault().id)
    }

    return try {
        val zdt: ZonedDateTime = dateTime.dateTime // Obtenemos el objeto

        // Convertimos ZonedDateTime a los tipos primitivos que espera TaskEntity
        val dateMillis = zdt.toInstant().toEpochMilli()
        val hour = zdt.hour * 60 + zdt.minute
        val timeZone = zdt.zone.id // El timeZone del propio ZonedDateTime

        Triple(dateMillis, hour, timeZone)

    } catch (e: Exception) {
        // Fallback en caso de error
        Triple(0L, 0, TimeZone.getDefault().id)
    }
}

/**
 * Helper REESCRITO para leer desde ZonedDateTime.
 */
private fun calculateDuration(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Int {
    if (start?.dateTime == null || end?.dateTime == null) return 0
    return try {
        // Usamos la clase Duration de java.time para calcular
        val duration = Duration.between(start.dateTime, end.dateTime)
        duration.toMinutes().toInt()
    } catch (e: Exception) { 0 }
}