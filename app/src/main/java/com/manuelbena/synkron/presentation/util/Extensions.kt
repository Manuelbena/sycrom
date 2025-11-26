package com.manuelbena.synkron.presentation.util

import com.manuelbena.synkron.R
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.presentation.models.CategoryType
import java.time.Duration

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

// Mapeo seguro y rápido: String del Dominio -> Recurso de UI
fun String.getCategoryIcon(): Int {
    // Normalizamos el string para evitar errores de mayúsculas/minúsculas
    return when (this.uppercase()) {
        "WORK", "TRABAJO" -> R.drawable.ic_work
        "PERSONAL" -> R.drawable.ic_home_black_24dp // Asegúrate de tener este drawable
        "HEALTH", "SALUD" -> R.drawable.ic_health
        "STUDIES", "ESTUDIOS" -> R.drawable.ic_book
        "FINANCE", "DINERO" -> R.drawable.ic_banck
        else -> R.drawable.ic_other // Fallback seguro por si llega algo raro
    }
}

fun String.getCategoryColor(): Int {
    return when (this.uppercase()) {
        "WORK", "TRABAJO" -> R.color.cat_work
        "PERSONAL" -> R.color.cat_personal
        "HEALTH", "SALUD" -> R.color.cat_health
        "STUDIES", "ESTUDIOS" -> R.color.cat_studies
        "FINANCE", "DINERO" -> R.color.cat_finance
        else -> R.color.cat_default
    }
}

// Lo mismo para el color si lo necesitas
fun String.getPriorityColor(): Int {
    return when (this.uppercase()) {
        "HIGH", "ALTA" -> R.color.priority_high
        "MEDIUM", "MEDIA" -> R.color.priority_medium
        "LOW", "BAJA" -> R.color.priority_low
        else -> R.color.priority_default
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

fun formatTime(hour: Int, minute: Int): String {
    return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
