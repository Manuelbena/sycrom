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
import com.manuelbena.synkron.domain.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections
import javax.inject.Inject

class GoogleCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 1. LEER EVENTOS (Soporta múltiples calendarios)
    suspend fun fetchEventsBetween(startMillis: Long, endMillis: Long): List<TaskDomain> = withContext(Dispatchers.IO) {
        val tasks = mutableListOf<TaskDomain>()
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            // --- CORRECCIÓN 1: Comprobamos que account.account no sea null ---
            if (account != null && account.account != null) {

                // Usamos !! porque ya verificamos arriba que no es null
                val service = getCalendarService(account.account!!)

                val minTime = DateTime(startMillis)
                val maxTime = DateTime(endMillis)

                // Listar calendarios
                val calendarList = service.calendarList().list().execute()

                calendarList.items?.forEach { calendarEntry ->
                    try {
                        val events = service.events().list(calendarEntry.id)
                            .setMaxResults(50)
                            .setTimeMin(minTime)
                            .setTimeMax(maxTime)
                            .setSingleEvents(true)
                            .execute()

                        events.items?.forEach { event ->
                            // Usamos el nombre del calendario como origen
                            tasks.add(mapGoogleEventToTaskDomain(event, calendarEntry.summary ?: "Google"))
                        }
                    } catch (e: Exception) {
                        Log.e("SycromRepo", "Error leyendo calendario ${calendarEntry.summary}: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext tasks
    }

    // 2. INSERTAR EVENTO (Devuelve el ID de Google)
    suspend fun insertEvent(task: TaskDomain): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            // --- CORRECCIÓN 2: Misma comprobación de seguridad ---
            if (account != null && account.account != null) {

                val service = getCalendarService(account.account!!)

                val event = Event().apply {
                    summary = task.summary
                    description = task.description
                    // Mapeo seguro de fechas con bloques if/else completos
                    if (task.start != null) {
                        start = mapToEventDateTime(task.start)
                    }
                    if (task.end != null) {
                        end = mapToEventDateTime(task.end)
                    }
                }

                val createdEvent = service.events().insert("primary", event)
                    .setConferenceDataVersion(1)
                    .execute()

                Log.d("SycromRepo", "✅ Evento creado en Google. ID: ${createdEvent.id}")
                return@withContext createdEvent.id
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e("SycromRepo", "❌ Error insertando: ${e.message}")
            return@withContext null
        }
    }

    // --- HELPERS PRIVADOS ---

    private fun getCalendarService(account: android.accounts.Account): Calendar {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(CalendarScopes.CALENDAR)
        )
        credential.selectedAccount = account
        return Calendar.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("Sycrom")
            .build()
    }

    private fun mapGoogleEventToTaskDomain(event: Event, sourceName: String): TaskDomain {
        return TaskDomain(
            id = event.id.hashCode(), // ID temporal para la UI

            // ¡IMPORTANTE! Aquí guardamos el ID real de Google para la sincronización
            googleCalendarId = event.id,

            typeTask = sourceName,
            priority = "Media",
            summary = event.summary ?: "Sin título",
            description = event.description,
            location = event.location,
            colorId = event.colorId,
            start = mapEventDateTime(event.start),
            end = mapEventDateTime(event.end),
            attendees = mapAttendees(event.attendees),
            recurrence = event.recurrence ?: emptyList(),
            transparency = event.transparency ?: "opaque",
            conferenceLink = event.hangoutLink
        )
    }

    // ARREGLO DEL ERROR DEL "IF": Usamos bloques completos con return
    private fun mapEventDateTime(googleDate: EventDateTime?): GoogleEventDateTime? {
        if (googleDate == null) {
            return null
        }
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