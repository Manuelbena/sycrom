package com.manuelbena.synkron.presentation.util.extensions

import java.util.Locale

/**
 * Formatea la duración en minutos a un string "X h Y min" o "Y min".
 */
 fun String.formatDuration(durationInMinutes: Int) : String{
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

/**
 * Formatea la hora (total de minutos desde medianoche) a un string "HH:MM".
 */
 fun String.formatHour(totalMinutes: Int){
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
     this.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
}