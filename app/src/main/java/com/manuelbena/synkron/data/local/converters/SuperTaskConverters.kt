package com.manuelbena.synkron.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manuelbena.synkron.domain.models.SubTaskItem
import com.manuelbena.synkron.domain.models.SuperTaskType

class SuperTaskConverters {

    private val gson = Gson()

    // --- Convertidor para la lista de Subtareas ---
    @TypeConverter
    fun fromSubTaskList(value: List<SubTaskItem>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSubTaskList(value: String): List<SubTaskItem> {
        val listType = object : TypeToken<List<SubTaskItem>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // --- Convertidor para el Enum de Tipos ---
    @TypeConverter
    fun fromSuperTaskType(value: SuperTaskType): String {
        return value.name
    }

    @TypeConverter
    fun toSuperTaskType(value: String): SuperTaskType {
        return try {
            SuperTaskType.valueOf(value)
        } catch (e: Exception) {
            SuperTaskType.GYM // Valor por defecto si falla
        }
    }
}