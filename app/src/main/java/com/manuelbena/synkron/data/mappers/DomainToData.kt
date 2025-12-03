package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.TaskDomain
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone

fun TaskDomain.toEntity(): TaskEntity {

    // 1. Obtener TimeZone y Timestamps (Manejo seguro de nulos)
    val timeZoneId = this.start?.timeZone ?: java.util.TimeZone.getDefault().id
    val zoneId = try {
        ZoneId.of(timeZoneId)
    } catch (e: Exception) {
        ZoneId.systemDefault()
    }

    val startMillis = this.start?.dateTime ?: System.currentTimeMillis()
    val endMillis = this.end?.dateTime ?: (startMillis + 3600000) // Default 1h de duración

    // 2. Convertir a objetos temporales SOLO para calcular 'date' y 'hour'
    // (Necesario para saber cuándo empieza el día según la zona horaria)
    val startInstant = Instant.ofEpochMilli(startMillis)
    val startZoned = ZonedDateTime.ofInstant(startInstant, zoneId)

    // A. Campo 'date': Inicio del día (00:00) en milisegundos
    val startOfDayMillis = startZoned.toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()

    // B. Campo 'hour': Minutos desde la medianoche
    val minutesFromMidnight = (startZoned.hour * 60) + startZoned.minute

    // C. Campo 'duration': Diferencia en minutos (Matemática simple con Long)
    val durationMinutes = (endMillis - startMillis) / 60000

    // 3. Crear la entidad
    return TaskEntity(
        id = this.id,

        // Campos calculados
        date = startOfDayMillis,      // Long (Milis al inicio del día)
        hour = minutesFromMidnight,   // Int (Minutos del día)
        duration = durationMinutes.toInt(),
        timeZone = timeZoneId,

        // Mapeo directo de campos
        summary = this.summary,
        description = this.description,
        location = this.location,
        colorId = this.colorId,

        // Listas (Tu TaskEntity ya debe tener converters para esto)
        attendeesEmails = this.attendees.map { it.email },
        reminderMinutes = this.reminders.overrides.map { it.minutes },
        recurrenceRule = this.recurrence.firstOrNull(), // Tomamos la primera regla si existe

        transparency = this.transparency,
        conferenceLink = this.conferenceLink,
        categoryIcon = this.categoryIcon,
        categoryColor = this.categoryColor,
        subTasks = this.subTasks,
        typeTask = this.typeTask,
        priority = this.priority,

        // Estados booleanos
        isActive = this.isActive,
        isDone = this.isDone,
        isDeleted = this.isDeleted,
        isArchived = this.isArchived,
        isPinned = this.isPinned,

        // Recurrencia propia de Synkrón
        synkronRecurrence = this.synkronRecurrence,
        synkronRecurrenceDays = this.synkronRecurrenceDays
    )
}


