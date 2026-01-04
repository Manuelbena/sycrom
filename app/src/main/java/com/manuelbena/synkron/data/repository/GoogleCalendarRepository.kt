package com.manuelbena.synkron.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.calendar.model.EventReminder
import com.manuelbena.synkron.domain.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID
import javax.inject.Inject

class GoogleCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ----------------------------------------------------------------
    // 1. LEER EVENTOS (Fetch)
    // ----------------------------------------------------------------
    suspend fun fetchEventsBetween(startMillis: Long, endMillis: Long): List<TaskDomain> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<TaskDomain>()
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val service = getCalendarService(account.account!!)

                val minTime = DateTime(startMillis)
                val maxTime = DateTime(endMillis)

                val calendarList = service.calendarList().list().execute()

                calendarList.items?.forEach { calendarEntry ->
                    try {
                        val events = service.events().list(calendarEntry.id)
                            .setTimeMin(minTime)
                            .setTimeMax(maxTime)
                            .setSingleEvents(true) // Importante: Expande eventos recurrentes
                            .setOrderBy("startTime")
                            .execute()

                        events.items?.forEach { event ->
                            if (event.status != "cancelled") {
                                // Aquí llamamos al mapper que recupera las subtareas
                                tasks.add(mapGoogleEventToTaskDomain(event, calendarEntry.summary ?: "Google"))
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SycromRepo", "Error en calendario ${calendarEntry.id}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SycromRepo", "Error general fetch: ${e.message}")
        }
        return@withContext tasks
    }

    // ----------------------------------------------------------------
    // 2. INSERTAR EVENTO (Insert)
    // ----------------------------------------------------------------
    suspend fun insertEvent(task: TaskDomain): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val service = getCalendarService(account.account!!)

                // A. CONSTRUIR DESCRIPCIÓN CON SUBTAREAS 📝
                val descriptionBuilder = StringBuilder()

                // 1. Descripción base
                if (!task.description.isNullOrEmpty()) {
                    descriptionBuilder.append(task.description)
                }

                // 2. Añadimos las subtareas al final
                if (task.subTasks.isNotEmpty()) {
                    if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append("\n\n")
                    descriptionBuilder.append("Subtareas:\n")
                    task.subTasks.forEach { sub ->
                        val check = if (sub.isDone) "[x]" else "[ ]"
                        descriptionBuilder.append("$check ${sub.title}\n")
                    }
                }

                // B. PROPIEDADES EXTENDIDAS (Guarda tipo ALARM) ⏰
                val extendedProps = Event.ExtendedProperties().apply {
                    private = mapOf("synkronType" to task.synkronRecurrence.name)
                }

                // C. RECORDATORIOS 🔔
                val eventReminders = Event.Reminders().apply {
                    useDefault = task.reminders.useDefault
                    if (!useDefault) {
                        overrides = task.reminders.overrides.map {
                            EventReminder().setMethod(it.method).setMinutes(it.minutes)
                        }
                    }
                }

                val event = Event().apply {
                    summary = task.summary
                    description = descriptionBuilder.toString()
                    location = task.location

                    // Color según categoría
                    colorId = mapCategoryToColorId(task.typeTask)

                    extendedProperties = extendedProps
                    reminders = eventReminders

                    if (task.start != null) start = mapToEventDateTime(task.start)
                    if (task.end != null) end = mapToEventDateTime(task.end)

                    if (task.synkronRecurrenceDays.isNotEmpty()) {
                        recurrence = createRecurrenceRule(task.synkronRecurrenceDays)
                    }
                }

                val createdEvent = service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute()

                return@withContext createdEvent.id
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e("SycromRepo", "Error insertando: ${e.message}")
            return@withContext null
        }
    }

    // ----------------------------------------------------------------
    // 3. BORRAR EVENTOS
    // ----------------------------------------------------------------
    suspend fun deleteEvent(googleId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val service = getCalendarService(account.account!!)
                service.events().delete("primary", googleId).execute()
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
    }

    // ----------------------------------------------------------------
    // MAPPERS Y PARSERS (La lógica de reconstrucción)
    // ----------------------------------------------------------------

    private fun mapGoogleEventToTaskDomain(event: Event, sourceName: String): TaskDomain {

        // 1. Recuperar Tipo (Alarma/Notificación)
        val typeString = event.extendedProperties?.private?.get("synkronType")
        val restoredType = try {
            if (typeString != null) NotificationType.valueOf(typeString) else NotificationType.NOTIFICATION
        } catch (e: Exception) { NotificationType.NOTIFICATION }

        // 2. RECUPERAR SUBTAREAS DESDE LA DESCRIPCIÓN 🕵️‍♂️
        // Esta función separa la descripción humana de la lista de tareas
        val (cleanDescription, parsedSubTasks) = parseDescriptionAndSubtasks(event.description)

        return TaskDomain(
            id = event.id.hashCode(),
            googleCalendarId = event.id,

            summary = event.summary ?: "(Sin título)",
            description = cleanDescription, // Descripción limpia (sin el bloque de subtareas)
            subTasks = parsedSubTasks,      // ¡Aquí se llenan tus subtareas!

            typeTask = sourceName,
            priority = "Media",

            synkronRecurrence = restoredType, // Tu campo restaurado

            colorId = event.colorId,
            start = mapEventDateTime(event.start),
            end = mapEventDateTime(event.end),
            location = event.location,
            attendees = mapAttendees(event.attendees),
            recurrence = event.recurrence ?: emptyList(),
            transparency = event.transparency ?: "opaque",
            conferenceLink = event.hangoutLink
        )
    }

    // --- PARSER INTELIGENTE DE SUBTAREAS ---
    private fun parseDescriptionAndSubtasks(fullDescription: String?): Pair<String?, List<SubTaskDomain>> {
        if (fullDescription.isNullOrEmpty()) return Pair(null, emptyList())

        // Buscamos la palabra clave "Subtareas:" (ignora mayúsculas/minúsculas)
        val marker = "Subtareas:"
        val index = fullDescription.indexOf(marker, ignoreCase = true)

        if (index == -1) {
            // Si no hay marcador, todo es descripción
            return Pair(fullDescription, emptyList())
        }

        // Dividimos el texto: Antes del marcador es la descripción, después son las tareas
        val descriptionPart = fullDescription.substring(0, index).trim()
        val subtasksPart = fullDescription.substring(index + marker.length)

        val subTasks = mutableListOf<SubTaskDomain>()

        // Procesamos línea por línea buscando [ ] o [x]
        subtasksPart.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {

                var isDone = false
                var title = trimmed

                // Detectamos patrones: [x], [ ], - [ ], etc.
                if (trimmed.startsWith("[x]", ignoreCase = true)) {
                    isDone = true
                    title = trimmed.substring(3).trim()
                } else if (trimmed.startsWith("[ ]")) {
                    isDone = false
                    title = trimmed.substring(3).trim()
                } else if (trimmed.startsWith("- [ ]") || trimmed.startsWith("* [ ]")) {
                    isDone = false
                    title = trimmed.substring(5).trim()
                }

                // Solo añadimos si hemos detectado un formato de tarea válido o si tiene contenido
                // (Para ser flexible, aceptamos líneas que empiezan con "-" o "*" como tareas simples)
                if (title.isNotEmpty()) {
                    if (title == trimmed && (trimmed.startsWith("- ") || trimmed.startsWith("* "))) {
                        title = trimmed.substring(2).trim()
                    }

                    // Evitamos añadir líneas vacías o basura
                    if (title.isNotEmpty()) {
                        subTasks.add(SubTaskDomain(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            isDone = isDone
                        ))
                    }
                }
            }
        }

        val finalDesc = if (descriptionPart.isEmpty()) null else descriptionPart
        return Pair(finalDesc, subTasks)
    }

    // --- HELPERS (Config, Colores, Fechas) ---

    private fun getCalendarService(account: android.accounts.Account): Calendar {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(CalendarScopes.CALENDAR)
        )
        credential.selectedAccount = account
        return Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Sycrom")
            .build()
    }

    private fun mapCategoryToColorId(category: String): String {
        return when (category.uppercase().trim()) {
            "WORK", "TRABAJO" -> "9"    // Azul
            "PERSONAL" -> "10"          // Verde
            "HEALTH", "SALUD" -> "11"   // Rojo
            "STUDY", "ESTUDIOS" -> "6"  // Naranja
            "MONEY", "FINANZAS" -> "3"  // Morado
            else -> "7"                 // Azul claro
        }
    }

    private fun createRecurrenceRule(days: List<Int>): List<String>? {
        if (days.isEmpty()) return null
        val dayCodes = days.mapNotNull { dayId ->
            when (dayId) {
                1 -> "MO"; 2 -> "TU"; 3 -> "WE"; 4 -> "TH"; 5 -> "FR"; 6 -> "SA"; 7 -> "SU"
                else -> null
            }
        }
        if (dayCodes.isEmpty()) return null
        return listOf("RRULE:FREQ=WEEKLY;BYDAY=${dayCodes.joinToString(",")}")
    }

    private fun mapEventDateTime(googleDate: EventDateTime?): GoogleEventDateTime? {
        if (googleDate == null) return null
        return GoogleEventDateTime(
            date = googleDate.date?.toString(),
            dateTime = googleDate.dateTime?.value,
            timeZone = googleDate.timeZone
        )
    }

    private fun mapToEventDateTime(domainDate: GoogleEventDateTime): EventDateTime {
        val eventDateTime = EventDateTime()
        if (domainDate.dateTime != null) {
            eventDateTime.dateTime = DateTime(domainDate.dateTime)
        } else if (domainDate.date != null) {
            eventDateTime.date = DateTime(domainDate.date)
        }
        eventDateTime.timeZone = domainDate.timeZone ?: "UTC"
        return eventDateTime
    }

    private fun mapAttendees(googleAttendees: List<com.google.api.services.calendar.model.EventAttendee>?): List<GoogleEventAttendee> {
        return googleAttendees?.map {
            GoogleEventAttendee(email = it.email, responseStatus = it.responseStatus)
        } ?: emptyList()
    }
}