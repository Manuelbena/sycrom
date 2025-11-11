package com.manuelbena.synkron.presentation.util

import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Formateador para parsear las fechas ISO 8601 que vienen de TaskDomain.
private val isoFormatter by lazy {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
}

/**
 * Convierte un [GoogleEventDateTime] (que tiene una fecha ISO)
 * en un string de hora simple (ej. "10:30").
 */
fun GoogleEventDateTime?.toHourString(locale: Locale = Locale.getDefault()): String {
    if (this == null) return "--:--"
    return try {
        val date = isoFormatter.parse(this.dateTime)
        if (date != null) {
            val timeFormatter = SimpleDateFormat("HH:mm", locale)
            timeFormatter.format(date)
        } else {
            "--:--"
        }
    } catch (e: Exception) {
        "--:--"
    }
}

/**
 * Calcula la duración en minutos entre dos objetos [GoogleEventDateTime].
 */
fun getDurationInMinutes(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Int {
    if (start == null || end == null) {
        return 0
    }
    return try {
        val startDate = isoFormatter.parse(start.dateTime)
        val endDate = isoFormatter.parse(end.dateTime)
        if (startDate != null && endDate != null) {
            val diffMillis = endDate.time - startDate.time
            // Convertir milisegundos a minutos
            (diffMillis / (1000 * 60)).toInt()
        } else {
            0
        }
    } catch (e: Exception) {
        0
    }
}

/**
 * Convierte un objeto [Calendar] de Java al objeto [GoogleEventDateTime]
 * que nuestro TaskDomain (y Google Calendar) espera.
 */
fun Calendar.toGoogleEventDateTime(): GoogleEventDateTime {
    val timeZoneId = this.timeZone.id
    val tz = TimeZone.getTimeZone(timeZoneId)
    isoFormatter.timeZone = tz

    return GoogleEventDateTime(
        dateTime = isoFormatter.format(this.time),
        timeZone = timeZoneId
    )
}

/**
 * Convierte un [GoogleEventDateTime] a un [Calendar] de Java.
 * Esencial para que la UI pueda manejar las fechas.
 */
fun GoogleEventDateTime?.toCalendar(): Calendar {
    if (this == null) return Calendar.getInstance()
    return try {
        val date = isoFormatter.parse(this.dateTime)
        Calendar.getInstance().apply {
            if (date != null) {
                time = date
            }
        }
    } catch (e: Exception) {
        Calendar.getInstance() // Devuelve 'now' si falla el parseo
    }
}

/**
 * Convierte un Int (minutos) en un string de duración (ej. "1 h 30 min").
 */
fun Int.toDurationString(): String {
    val durationInMinutes = this
    return when {
        durationInMinutes <= 0 -> "Sin Duración"
        durationInMinutes >= 60 -> {
            val hours = durationInMinutes / 60
            val minutes = durationInMinutes % 60
            if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
        }
        else -> "${durationInMinutes} min"
    }
}