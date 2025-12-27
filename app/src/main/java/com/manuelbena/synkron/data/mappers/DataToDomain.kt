package com.manuelbena.synkron.data.mappers

import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse
import com.manuelbena.synkron.data.remote.n8n.models.N8nSubTaskDto
import com.manuelbena.synkron.domain.models.GoogleEventAttendee
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.GoogleEventReminder
import com.manuelbena.synkron.domain.models.GoogleEventReminders
import com.manuelbena.synkron.domain.models.SubTaskDomain
import com.manuelbena.synkron.domain.models.TaskDomain
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import java.time.Instant
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID
import com.squareup.moshi.Types
import java.time.ZoneId
import java.time.format.DateTimeFormatter


fun TaskEntity.toDomain(): TaskDomain {

    // [DETECCIÓN SIN FECHA] Si la fecha es 0, start y end son NULL
    val isNoDate = (this.date == 0L)

    val startDateTime = if (isNoDate) null else createGoogleEventDateTime(this.date, this.hour, this.timeZone)

    val endDateTime = if (isNoDate) {
        null
    } else if (this.hour == -1) {
        // Todo el día: Fin igual al inicio
        createGoogleEventDateTime(this.date, -1, this.timeZone)
    } else {
        // Hora normal: Sumamos duración
        val endTimeMillis = this.date + (this.duration * 60 * 1000)
        createGoogleEventDateTime(endTimeMillis, -2, this.timeZone)
    }

    // Mapeo de listas auxiliares
    val attendees = this.attendeesEmails.map { GoogleEventAttendee(it) }
    val reminders = GoogleEventReminders(
        useDefault = false,
        overrides = this.reminderMinutes.map { GoogleEventReminder("popup", it) }
    )
    val recurrence = if (this.recurrenceRule != null) listOf(this.recurrenceRule) else emptyList()

    return TaskDomain(
        id = this.id,
        summary = this.summary,
        description = this.description,
        location = this.location,
        colorId = this.colorId,
        start = startDateTime,
        end = endDateTime,
        attendees = attendees,
        recurrence = recurrence,
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
        isPinned = this.isPinned,
        synkronRecurrence = this.synkronRecurrence,
        synkronRecurrenceDays = this.synkronRecurrenceDays
    )
}

// --- LÓGICA DE RECONSTRUCCIÓN ---

private fun createGoogleEventDateTime(dateMillis: Long, hourMinutes: Int, timeZone: String): GoogleEventDateTime {
    val zoneId = try { ZoneId.of(timeZone) } catch (e: Exception) { ZoneId.systemDefault() }

    return if (hourMinutes == -1) {
        // CASO: TODO EL DÍA
        val localDate = Instant.ofEpochMilli(dateMillis).atZone(zoneId).toLocalDate()
        GoogleEventDateTime(
            dateTime = null,
            date = localDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
            timeZone = timeZone
        )
    } else {
        // CASO: CON HORA
        GoogleEventDateTime(
            dateTime = dateMillis,
            date = null,
            timeZone = timeZone
        )
    }
}
// --- FUNCIONES CORREGIDAS PARA DEVOLVER LONG ---

// Caso 1: Ya tenemos los milisegundos exactos (para el 'end')
private fun createGoogleEventDateTime(dateMillis: Long, timeZone: String): GoogleEventDateTime {
    // ¡SIMPLE! Pasamos el Long directo.
    return GoogleEventDateTime(dateTime = dateMillis, timeZone = timeZone)
}


// Instancia de Moshi para parsear la lista interna.
// Idealmente esto se inyectaría, pero en un mapper estático podemos usar una instancia local lazy.
private val moshiParser by lazy {
    Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
}

fun N8nChatResponse.toTaskDomain(): TaskDomain {

    val startEvent = startTimestamp?.toLongOrNull()?.let { millis ->
        GoogleEventDateTime(
            dateTime = millis, // Ahora encaja perfecto porque ambos son Long
            timeZone = java.util.TimeZone.getDefault().id
        )
    }

    val endEvent = startEvent?.dateTime?.let { startMillis ->
        GoogleEventDateTime(
            dateTime = startMillis + 3600000, // +1 Hora
            timeZone = java.util.TimeZone.getDefault().id
        )
    }

    // 1. Parseo de Subtareas (String -> List<SubTaskDomain>)
    val parsedSubTasks = try {
        if (!subTasksString.isNullOrEmpty()) {
            val listType = Types.newParameterizedType(List::class.java, N8nSubTaskDto::class.java)
            val adapter = moshiParser.adapter<List<N8nSubTaskDto>>(listType)
            val dtos = adapter.fromJson(subTasksString) ?: emptyList()

            // Mapeamos el DTO interno al Domain de SubTask
            dtos.map { dto ->
                SubTaskDomain(
                    id = dto.id ?: UUID.randomUUID().toString(),
                    title = dto.title ?: "Sin título",
                    isDone = dto.isDone ?: false
                )
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList() // Fallback seguro
    }


    return TaskDomain(
        id = this.id?.toIntOrNull() ?: 0,
        summary = this.title ?: "Nueva Tarea IA",
        subTasks = parsedSubTasks,
        typeTask = this.typeTask ?: "PERSONAL",
        priority = this.priority ?: "Media",
        isActive = this.isActive.toBoolean(), // "true" -> true
        isDone = this.isDone.toBoolean(),
        description = this.description,
        location = this.location,
        start = startEvent,
        end = endEvent
    )
}