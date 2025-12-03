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


fun TaskEntity.toDomain(): TaskDomain {

    // 1. Reconstruir el objeto 'start'
    val startDateTime = createGoogleEventDateTime(this.date, this.hour, this.timeZone)

    // 2. Reconstruir el objeto 'end'
    val calendar = Calendar.getInstance().apply {
        timeInMillis = this@toDomain.date
        set(Calendar.HOUR_OF_DAY, this@toDomain.hour / 60)
        set(Calendar.MINUTE, this@toDomain.hour % 60)
    }
    val endTimeMillis = calendar.timeInMillis + (this.duration * 60 * 1000)

    // Aquí simplemente pasamos el Long directo
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
        synkronRecurrence = this.synkronRecurrence,
        synkronRecurrenceDays = this.synkronRecurrenceDays
    )
}

// --- FUNCIONES CORREGIDAS PARA DEVOLVER LONG ---

// Caso 1: Ya tenemos los milisegundos exactos (para el 'end')
private fun createGoogleEventDateTime(dateMillis: Long, timeZone: String): GoogleEventDateTime {
    // ¡SIMPLE! Pasamos el Long directo.
    return GoogleEventDateTime(dateTime = dateMillis, timeZone = timeZone)
}

// Caso 2: Tenemos fecha + hora en minutos (para el 'start')
private fun createGoogleEventDateTime(dateMillis: Long, hourMinutes: Int, timeZone: String): GoogleEventDateTime {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        set(Calendar.HOUR_OF_DAY, hourMinutes / 60)
        set(Calendar.MINUTE, hourMinutes % 60)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    // Obtenemos los millis del calendario y los metemos en el objeto
    return GoogleEventDateTime(dateTime = calendar.timeInMillis, timeZone = timeZone)
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

    // 2. Parseo de Fechas (Timestamp String -> GoogleEventDateTime)
    // Asumo que tu GoogleEventDateTime tiene un constructor o setter compatible.
    // Si no, ajusta esta parte según tu implementación de GoogleEventDateTime.
    val startDate = startTimestamp?.toLongOrNull()?.let { millis ->
        // Aquí necesitas convertir millis a tu objeto GoogleEventDateTime
        // Ejemplo genérico (ajústalo a tu clase real):
        // GoogleEventDateTime(dateTime = com.google.api.client.util.DateTime(millis))
        null // TODO: ¡Reemplazar con tu constructor real de GoogleEventDateTime!
    }

    return TaskDomain(
        id = this.id?.toIntOrNull() ?: 0,
        subTasks = parsedSubTasks,
        typeTask = this.typeTask ?: "PERSONAL",
        categoryIcon = this.categoryIcon ?: "ic_work",
        categoryColor = this.categoryColor ?: "#FF5722",
        priority = this.priority ?: "Media",
        isActive = this.isActive.toBoolean(), // "true" -> true
        isDone = this.isDone.toBoolean(),
        description = this.description,
        summary = this.title ?: "Nueva Tarea IA", // Usamos summary como título principal
        location = this.location,
        start = startEvent,
        end = null
    )
}