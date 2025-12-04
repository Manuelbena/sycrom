package com.manuelbena.synkron.presentation.util

import com.manuelbena.synkron.R
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- FUNCIONES DE FECHA REPARADAS Y SEGURAS ---

fun GoogleEventDateTime?.toHourString(locale: Locale = Locale.getDefault()): String {
    val millis = this?.dateTime ?: return "--:--"

    return try {
        // CORRECCIÓN: Usamos 'this?.timeZone' de forma segura
        val zoneId = try {
            this?.timeZone?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        } catch (e: Exception) {
            ZoneId.systemDefault()
        }

        val zonedDateTime = Instant.ofEpochMilli(millis).atZone(zoneId)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", locale)
        zonedDateTime.format(timeFormatter)
    } catch (e: Exception) {
        "--:--"
    }
}

fun getDurationInMinutes(start: GoogleEventDateTime?, end: GoogleEventDateTime?): Long {
    if (start == null || end == null) return 0L

    // Prioridad 1: Usar dateTime (fecha y hora exacta)
    if (start.dateTime != null && end.dateTime != null) {
        val diffMillis = end.dateTime - start.dateTime
        return TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    }

    // Prioridad 2: Usar date (evento de día completo)
    // Los eventos de todo el día suelen contar como 24h * días
    if (start.dateTime != null && end.dateTime != null) {
        // En Google Calendar, 'end.date' es exclusivo (el día siguiente).
        // Calculamos la diferencia en días y pasamos a minutos.
        // Nota: Esto es una aproximación simple.
        val diffMillis = end.dateTime - start.dateTime
        return TimeUnit.MILLISECONDS.toMinutes(diffMillis)
    }

    return 0L
}

fun Calendar.toGoogleEventDateTime(): GoogleEventDateTime {
    return GoogleEventDateTime(
        dateTime = this.timeInMillis,
        timeZone = this.timeZone.id
    )
}

fun GoogleEventDateTime?.toCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    val millis = this?.dateTime

    if (millis != null) {
        calendar.timeInMillis = millis
        // CORRECCIÓN: Usamos 'this?.timeZone' porque 'this' es nullable
        this?.timeZone?.let {
            try {
                calendar.timeZone = java.util.TimeZone.getTimeZone(it)
            } catch (_: Exception) {}
        }
    }
    return calendar
}

// --- RESTO DEL ARCHIVO (SIN CAMBIOS) ---

fun String.getCategoryIcon(): Int {
    return when (this.uppercase()) {
        "WORK", "TRABAJO" -> R.drawable.ic_work
        "PERSONAL" -> R.drawable.ic_home_black_24dp
        "HEALTH", "SALUD" -> R.drawable.ic_health
        "STUDIES", "ESTUDIOS" -> R.drawable.ic_book
        "FINANCE", "DINERO" -> R.drawable.ic_banck
        else -> R.drawable.ic_other
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

fun String.getPriorityColor(): Int {
    return when (this.uppercase()) {
        "HIGH", "ALTA" -> R.color.priority_high
        "MEDIUM", "MEDIA" -> R.color.priority_medium
        "LOW", "BAJA" -> R.color.priority_low
        else -> R.color.priority_default
    }
}

fun Int.toDurationString(): String {
    val durationInMinutes = this
    return when {
        durationInMinutes <= 0 -> ""
        durationInMinutes >= 60 -> {
            val hours = durationInMinutes / 60
            val minutes = durationInMinutes % 60
            if (minutes == 0) "${hours} h" else "${hours} h ${minutes} min"
        }
        else -> "${durationInMinutes} min"
    }
}

fun formatTime(hour: Int, minute: Int): String {
    return "%02d:%02d".format(hour, minute)
}