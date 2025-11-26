package com.manuelbena.synkron.data.local.converters

import androidx.room.TypeConverter
import com.manuelbena.synkron.domain.models.RecurrenceType

class RecurrenceConverter {

    // Para el Enum
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType {
        return try {
            RecurrenceType.valueOf(value)
        } catch (e: Exception) {
            RecurrenceType.NOTIFICATION
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