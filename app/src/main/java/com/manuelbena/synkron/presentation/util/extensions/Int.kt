package com.manuelbena.synkron.presentation.util.extensions

import java.util.Locale

/**
 * Convierte un [Int] (que representa minutos totales) en un string de duración
 * formateado (ej. "1h 30 min", "45 min" o "Sin Duración").
 *
 * Uso:
 * val duration = 90
 * val formatted = duration.toDurationString() // "1 h 30 min"
 */
fun Int.toDurationString(): String {
    val durationInMinutes = this
    return when {
        durationInMinutes <= 0 -> "Sin Duración"
        durationInMinutes >= 60 -> {
            val hours = durationInMinutes / 60
            val minutes = durationInMinutes % 60
            // Devuelve "X h" si los minutos son 0
            if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
        }
        else -> "${durationInMinutes} min"
    }
}

/**
 * Convierte un [Int] (que representa los minutos totales desde medianoche)
 * en un string de hora formateado (ej. "10:30").
 *
 * @param locale El Locale a usar para el formateo (por defecto, el del sistema).
 *
 * Uso:
 * val time = 630
 * val formatted = time.toHourString() // "10:30"
 */
fun Int.toHourString(locale: Locale = Locale.getDefault()): String {
    val totalMinutes = this
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format(locale, "%02d:%02d", hours, minutes)
}