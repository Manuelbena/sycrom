package com.manuelbena.synkron.data.local.converters

import androidx.room.TypeConverter
import com.manuelbena.synkron.domain.models.NotificationType

class RecurrenceConverter {

    // Para el Enum
    @TypeConverter
    fun fromRecurrenceType(value: NotificationType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String): NotificationType {
        return try {
            NotificationType.valueOf(value)
        } catch (e: Exception) {
            NotificationType.NOTIFICATION
        }
    }

    // Para la lista de días (List<Int>)
    @TypeConverter
    fun fromDaysList(list: List<Int>?): String {
        if (list.isNullOrEmpty()) return ""
        return list.joinToString(",")
    }

    @TypeConverter
    fun toDaysList(value: String?): List<Int> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(",").mapNotNull { it.toIntOrNull() }
    }
}