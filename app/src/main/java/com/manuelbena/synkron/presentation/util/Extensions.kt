package com.manuelbena.synkron.presentation.util

import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

// --- ¡ELIMINADO! ---
// private val isoFormatter by lazy { ... }
// No lo necesitamos si usamos ZonedDateTime correctamente.

/**
 * Convierte un [GoogleEventDateTime] en un string de hora simple (ej. "10:30").
 * REESCRITO para usar ZonedDateTime (y quitada la referencia a .date).
 */
fun GoogleEventDateTime?.toHourString(locale: Locale = Locale.getDefault()): String {
    // Usamos this?.dateTime que SÍ existe.
    if (this?.dateTime == null) {
        return "--:--" // Tu lógica original no manejaba 'date', así que la respetamos.
    }
    return try {
        // Usamos el formateador moderno
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
        this.dateTime.format(timeFormatter)
    } catch (e: Exception) {
        "--:--"
    }
}

/**
 * Calcula la duración en minutos entre dos objetos [GoogleEventDateTime].
 * REESCRITO para usar ZonedDateTime.
 */
fun getDurationInMinutes(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Int {
    if (start?.dateTime == null || end?.dateTime == null) {
        return 0
    }
    return try {
        // Usamos la clase Duration para calcular la diferencia
        val duration = Duration.between(start.dateTime, end.dateTime)
        duration.toMinutes().toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * Convierte un objeto [Calendar] de Java al objeto [GoogleEventDateTime]
 * REESCRITO para crear un ZonedDateTime.
 */
fun Calendar.toGoogleEventDateTime(): GoogleEventDateTime {
    // Convertimos Calendar a ZonedDateTime
    val zonedDateTime = this.toInstant().atZone(this.timeZone.toZoneId())

    return GoogleEventDateTime(
        dateTime = zonedDateTime,
        timeZone = zonedDateTime.zone.id
    )
}

/**
 * Convierte un [GoogleEventDateTime] a un [Calendar] de Java.
 * REESCRITO para leer desde ZonedDateTime.
 */
fun GoogleEventDateTime?.toCalendar(): Calendar {
    if (this?.dateTime == null) {
        return Calendar.getInstance() // Devuelve 'now' si no hay nada
    }
    return try {
        // Convertimos ZonedDateTime a GregorianCalendar
        GregorianCalendar.from(this.dateTime)
    } catch (e: Exception) {
        Calendar.getInstance() // Devuelve 'now' si falla
    }
}

/**
 * Convierte un Int (minutos) en un string de duración (ej. "1 h 30 min").
 * (Esta función ya estaba correcta)
 */
fun Int.toDurationString(): String {
    val durationInMinutes = this
    return when {
        durationInMinutes <= 0 -> "" // Cambiado de "Sin Duración" a "" para un look más limpio
        durationInMinutes >= 60 -> {
            val hours = durationInMinutes / 60
            val minutes = durationInMinutes % 60
            if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
        }
        else -> "${durationInMinutes} min"
    }
}