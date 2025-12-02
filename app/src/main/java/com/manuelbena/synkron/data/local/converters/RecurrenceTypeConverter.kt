package com.manuelbena.synkron.data.local.converters

import androidx.room.TypeConverter
import com.manuelbena.synkron.domain.models.NotificationType

class RecurrenceTypeConverter {
    @TypeConverter
    fun fromRecurrenceType(value: NotificationType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String?): NotificationType {
        return try {
            if (value != null) NotificationType.valueOf(value) else NotificationType.NOTIFICATION
        } catch (e: Exception) {
            NotificationType.NOTIFICATION
        }
    }
}