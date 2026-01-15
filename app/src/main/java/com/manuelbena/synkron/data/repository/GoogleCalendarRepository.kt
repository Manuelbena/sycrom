package com.manuelbena.synkron.data.repository

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.json.GoogleJsonResponseException
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.UUID
import javax.inject.Inject

class GoogleCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ----------------------------------------------------------------
    // 1. LEER EVENTOS
    // ----------------------------------------------------------------
    suspend fun fetchEventsBetween(startMillis: Long, endMillis: Long): List<TaskDomain> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<TaskDomain>()
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null && account.account != null) {
                val service = getService() ?: return@withContext emptyList()

                val minTime = DateTime(startMillis)
                val maxTime = DateTime(endMillis)

                val calendarList = service.calendarList().list().execute()

                // Recorremos los calendarios con delay para evitar bloqueo 403
                for (calendarEntry in calendarList.items ?: emptyList()) {
                    delay(200)

                    try {
                        val events = service.events().list(calendarEntry.id)
                            .setTimeMin(minTime)
                            .setTimeMax(maxTime)
                            .setSingleEvents(true)
                            .setOrderBy("startTime")
                            .execute()

                        events.items?.forEach { event ->
                            try {
                                if (event.status != "cancelled") {
                                    // Intentamos mapear. Si falla, el try-catch interno lo captura.
                                    val mappedTask = mapGoogleEventToTaskDomain(event, calendarEntry.summary ?: "Google")
                                    if (mappedTask != null) {
                                        tasks.add(mappedTask)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("SycromRepo", "⚠️ Saltando evento corrupto '${event.summary}': ${e.message}")
                            }
                        }
                    } catch (e: GoogleJsonResponseException) {
                        if (e.statusCode == 403) {
                            Log.w("SycromRepo", "🛑 Cuota Google excedida. Parando sync de este lote.")
                            break
                        } else {
                            Log.e("SycromRepo", "Error leyendo calendario: ${e.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("SycromRepo", "Error genérico calendario", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SycromRepo", "Error fatal fetch", e)
        }
        return@withContext tasks
    }

    // ----------------------------------------------------------------
    // 2. INSERTAR EVENTO
    // ----------------------------------------------------------------
    suspend fun insertEvent(task: TaskDomain): String? = withContext(Dispatchers.IO) {
        try {
            val service = getService() ?: return@withContext null
            val event = createGoogleEventObject(task)
            val createdEvent = service.events().insert("primary", event).setConferenceDataVersion(1).execute()
            return@withContext createdEvent.id
        } catch (e: Exception) {
            Log.e("SycromRepo", "Error insertando: ${e.message}")
            return@withContext null
        }
    }

    // ----------------------------------------------------------------
    // 3. ACTUALIZAR EVENTO
    // ----------------------------------------------------------------
    suspend fun updateEvent(task: TaskDomain): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getService() ?: return@withContext false
            val event = createGoogleEventObject(task)
            service.events().update("primary", task.googleCalendarId, event).execute()
            return@withContext true
        } catch (e: Exception) {
            Log.e("SycromRepo", "Error actualizando: ${e.message}")
            return@withContext false
        }
    }

    // ----------------------------------------------------------------
    // 4. BORRAR EVENTO
    // ----------------------------------------------------------------
    suspend fun deleteEvent(googleId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val service = getService() ?: return@withContext false
            service.events().delete("primary", googleId).execute()
            return@withContext true
        } catch (e: Exception) { return@withContext false }
    }

    // ----------------------------------------------------------------
    // MAPPERS Y HELPERS (LA SOLUCIÓN DEFINITIVA) 🛡️
    // ----------------------------------------------------------------

    // Esta función usa el operador ?. (safe call) para todo.
    // Si algo es null, devuelve null silenciosamente en vez de crashear.
    private fun mapEventDateTime(googleDate: EventDateTime?): GoogleEventDateTime? {
        if (googleDate == null) return null

        try {
            // Usamos la zona horaria del sistema si Google no nos da una
            val safeTimeZone = googleDate.timeZone ?: java.time.ZoneId.systemDefault().id

            // 1. Intentamos leer la hora exacta
            val dtValue = googleDate.dateTime?.value
            if (dtValue != null) {
                return GoogleEventDateTime(
                    dateTime = dtValue,
                    date = null,
                    timeZone = safeTimeZone // <--- AQUÍ ESTABA EL ERROR (antes era null)
                )
            }

            // 2. Intentamos leer la fecha de todo el día
            val dValue = googleDate.date?.toString()
            if (dValue != null) {
                return GoogleEventDateTime(
                    dateTime = null,
                    date = dValue,
                    timeZone = safeTimeZone // <--- AQUÍ TAMBIÉN
                )
            }
        } catch (e: Exception) {
            Log.e("SycromRepo", "Error interno mapeando fechas: ${e.message}")
        }
        return null
    }

    private fun mapGoogleEventToTaskDomain(event: Event, calendarName: String): TaskDomain? {
        // Mapeo seguro de fechas
        val startMapped = mapEventDateTime(event.start)
        val endMapped = mapEventDateTime(event.end)

        // Si no tiene fecha de inicio, devolvemos null (no lanzamos excepción)
        // para que el bucle simplemente lo ignore y siga con el siguiente.
        if (startMapped == null) {
            Log.w("SycromRepo", "Ignorando evento sin fecha válida: ${event.summary}")
            return null
        }

        // Resto del mapeo seguro
        val rawTitle = event.summary ?: "(Sin título)"
        val isDone = rawTitle.trim().startsWith("✅")
        val cleanTitle = if (isDone) rawTitle.replace("✅", "").trim() else rawTitle

        val detectedCategory = if (event.colorId != null) mapColorIdToCategory(event.colorId) else "Personal"

        val typeString = event.extendedProperties?.private?.get("synkronType")
        val restoredType = try {
            if (typeString != null) NotificationType.valueOf(typeString) else NotificationType.NOTIFICATION
        } catch (e: Exception) { NotificationType.NOTIFICATION }

        val (cleanDescription, parsedSubTasks) = parseDescriptionAndSubtasks(event.description)

        return TaskDomain(
            id = event.id.hashCode(),
            googleCalendarId = event.id,
            summary = cleanTitle,
            isDone = isDone,
            typeTask = detectedCategory,
            description = cleanDescription,
            subTasks = parsedSubTasks,
            colorId = event.colorId,
            start = startMapped,
            end = endMapped,
            location = event.location,
            attendees = mapAttendees(event.attendees),
            priority = "Media",
            synkronRecurrence = restoredType,
            synkronRecurrenceDays = parseGoogleRecurrence(event.recurrence),
            recurrence = event.recurrence ?: emptyList(),
            transparency = event.transparency ?: "opaque",
            conferenceLink = event.hangoutLink
        )
    }

    // --- Helpers de construcción (No cambiados, solo asegurados) ---

    private fun createGoogleEventObject(task: TaskDomain): Event {
        val finalTitle = if (task.isDone) "✅ ${task.summary}" else task.summary

        val descriptionBuilder = StringBuilder()
        if (!task.description.isNullOrEmpty()) descriptionBuilder.append(task.description)
        if (task.subTasks.isNotEmpty()) {
            if (descriptionBuilder.isNotEmpty()) descriptionBuilder.append("\n\n")
            descriptionBuilder.append("Subtareas:\n")
            task.subTasks.forEach { sub ->
                val check = if (sub.isDone) "[x]" else "[ ]"
                descriptionBuilder.append("$check ${sub.title}\n")
            }
        }

        val extendedProps = Event.ExtendedProperties().apply {
            private = mapOf("synkronType" to task.synkronRecurrence.name)
        }

        val eventReminders = Event.Reminders().apply {
            useDefault = task.reminders.useDefault
            if (!useDefault) {
                overrides = task.reminders.overrides.map {
                    EventReminder().setMethod(it.method).setMinutes(it.minutes)
                }
            }
        }

        return Event().apply {
            summary = finalTitle
            description = descriptionBuilder.toString()
            location = task.location
            colorId = mapCategoryToColorId(task.typeTask)
            extendedProperties = extendedProps
            reminders = eventReminders
            if (task.start != null) start = mapToEventDateTime(task.start)
            if (task.end != null) end = mapToEventDateTime(task.end)
            if (task.synkronRecurrenceDays.isNotEmpty()) {
                recurrence = createRecurrenceRule(task.synkronRecurrenceDays)
            }
        }
    }

    private fun parseDescriptionAndSubtasks(fullDescription: String?): Pair<String?, List<SubTaskDomain>> {
        if (fullDescription.isNullOrEmpty()) return Pair(null, emptyList())
        val marker = "Subtareas:"
        val index = fullDescription.indexOf(marker, ignoreCase = true)
        if (index == -1) return Pair(fullDescription, emptyList())

        val descriptionPart = fullDescription.substring(0, index).trim()
        val subtasksPart = fullDescription.substring(index + marker.length)
        val subTasks = mutableListOf<SubTaskDomain>()

        subtasksPart.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                var isDone = false
                var title = trimmed
                if (trimmed.startsWith("[x]", ignoreCase = true)) {
                    isDone = true; title = trimmed.substring(3).trim()
                } else if (trimmed.startsWith("[ ]")) {
                    isDone = false; title = trimmed.substring(3).trim()
                } else if (trimmed.startsWith("- [ ]") || trimmed.startsWith("* [ ]")) {
                    isDone = false; title = trimmed.substring(5).trim()
                } else if (trimmed.startsWith("- ")) {
                    isDone = false; title = trimmed.substring(2).trim()
                }

                if (title.isNotEmpty()) {
                    subTasks.add(SubTaskDomain(id = UUID.randomUUID().toString(), title = title, isDone = isDone))
                }
            }
        }
        val finalDesc = if (descriptionPart.isEmpty()) null else descriptionPart
        return Pair(finalDesc, subTasks)
    }

    private fun mapCategoryToColorId(category: String): String {
        return when (category.uppercase().trim()) {
            "TRABAJO", "WORK" -> "9"; "PERSONAL" -> "10"; "SALUD", "HEALTH" -> "11"
            "ESTUDIOS", "STUDY" -> "6"; "FINANZAS", "MONEY" -> "3"; "OCIO", "LEISURE" -> "5"
            else -> "7"
        }
    }

    private fun mapColorIdToCategory(colorId: String?): String {
        return when (colorId) {
            "9" -> "Trabajo"; "10" -> "Personal"; "11" -> "Salud"
            "6" -> "Estudios"; "3" -> "Finanzas"; "5" -> "Ocio"
            else -> "Personal"
        }
    }

    private fun getService(): Calendar? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(CalendarScopes.CALENDAR))
        credential.selectedAccount = account.account
        return Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Sycrom")
            .build()
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
        return googleAttendees?.map { GoogleEventAttendee(email = it.email, responseStatus = it.responseStatus) } ?: emptyList()
    }

    private fun parseGoogleRecurrence(recurrenceList: List<String>?): List<Int> {
        if (recurrenceList.isNullOrEmpty()) return emptyList()
        val days = mutableListOf<Int>()
        recurrenceList.forEach { rule ->
            if (rule.startsWith("RRULE:")) {
                val parts = rule.split(";")
                val byDayPart = parts.find { it.startsWith("BYDAY=") }
                if (byDayPart != null) {
                    val daysString = byDayPart.removePrefix("BYDAY=")
                    daysString.split(",").forEach { dayCode ->
                        val cleanCode = dayCode.takeLast(2)
                        when (cleanCode) {
                            "MO" -> days.add(1); "TU" -> days.add(2); "WE" -> days.add(3)
                            "TH" -> days.add(4); "FR" -> days.add(5); "SA" -> days.add(6); "SU" -> days.add(7)
                        }
                    }
                }
            }
        }
        return days
    }

    private fun createRecurrenceRule(days: List<Int>): List<String>? {
        if (days.isEmpty()) return null
        val dayCodes = days.mapNotNull { dayId ->
            when (dayId) {
                1 -> "MO"; 2 -> "TU"; 3 -> "WE"; 4 -> "TH"; 5 -> "FR"; 6 -> "SA"; 7 -> "SU"
                else -> null
            }
        }
        return listOf("RRULE:FREQ=WEEKLY;BYDAY=${dayCodes.joinToString(",")}")
    }
}