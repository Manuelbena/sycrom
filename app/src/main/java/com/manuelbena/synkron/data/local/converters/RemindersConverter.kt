// Archivo: data/local/RemindersConverter.kt
package com.manuelbena.synkron.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manuelbena.synkron.domain.models.GoogleEventReminders

class RemindersConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromReminders(reminders: GoogleEventReminders?): String? {
        if (reminders == null) return null
        // Convierte el objeto a un Texto JSON para guardarlo en la BD
        return gson.toJson(reminders)
    }

    @TypeConverter
    fun toReminders(remindersString: String?): GoogleEventReminders? {
        if (remindersString.isNullOrEmpty()) return null
        // Convierte el Texto JSON de vuelta a tu Objeto original
        val type = object : TypeToken<GoogleEventReminders>() {}.type
        return gson.fromJson(remindersString, type)
    }
}