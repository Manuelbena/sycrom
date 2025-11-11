package com.manuelbena.synkron.presentation.util

import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ... (aquí deben estar tus otras funciones, como toGoogleEventDateTime) ...

/**
 * Formateador para parsear las fechas ISO 8601 que vienen de TaskDomain.
 */
private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())

/**
 * Convierte un [GoogleEventDateTime] a un [Calendar] de Java.
 * Esencial para que la UI pueda manejar las fechas.
 */
fun GoogleEventDateTime.toCalendar(): Calendar {
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
 * Convierte un [GoogleEventDateTime] (que tiene una fecha ISO)
 * en un string de hora simple (ej. "10:30").
 */
fun GoogleEventDateTime.toHourString(locale: Locale = Locale.getDefault()): String {
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