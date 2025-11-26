package com.manuelbena.synkron.data.local.converters

import androidx.room.TypeConverter
import com.manuelbena.synkron.domain.models.RecurrenceType

class RecurrenceTypeConverter {
    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String?): RecurrenceType {
        return try {
            if (value != null) RecurrenceType.valueOf(value) else RecurrenceType.NOTIFICATION
        } catch (e: Exception) {
            RecurrenceType.NOTIFICATION
        }
    }
}