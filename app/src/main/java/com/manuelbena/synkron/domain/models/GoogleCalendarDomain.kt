package com.manuelbena.synkron.domain.models // O donde prefieras

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.time.ZonedDateTime

/**
 * Representa el objeto 'start' o 'end' en un Evento de Google Calendar.
 *
 * @property dateTime La fecha y hora en formato ISO 8601 (RFC3339).
 * Ej: "2025-11-20T10:30:00+01:00"
 * @property timeZone El Timezone IANA. Ej: "Europe/Madrid"
 */
@Parcelize
data class GoogleEventDateTime(

    val dateTime: Long? = null,
    val timeZone: String
) : Parcelable

/**
 * Representa un 'attendee' (invitado) en un Evento de Google Calendar.
 *
 * @property email El email del invitado.
 * @property responseStatus El estado de su respuesta.
 * Ej: "needsAction", "accepted", "declined", "tentative"
 */
@Parcelize
data class GoogleEventAttendee(
    val email: String,
    val responseStatus: String = "needsAction"
) : Parcelable

/**
 * Representa un 'reminder' (recordatorio) en un Evento de Google Calendar.
 *
 * @property method Tipo de recordatorio. Ej: "popup", "email"
 * @property minutes Minutos antes del evento.
 */
@Parcelize
data class GoogleEventReminder(
    val method: String = "popup",
    val minutes: Int,
    val message: String? = null
) : Parcelable

/**
 * Representa el objeto 'reminders' de Google Calendar, que contiene
 * la lista de 'overrides' (recordatorios personalizados).
 */
@Parcelize
data class GoogleEventReminders(
    val useDefault: Boolean = false,
    val overrides: List<GoogleEventReminder> = emptyList()
) : Parcelable

// NOTA: El 'conferenceData' es un objeto muy complejo.
// Para mantener la simplicidad, seguiremos usando un simple 'conferenceLink: String?'
// en nuestro TaskDomain, que es lo que realmente nos importa.