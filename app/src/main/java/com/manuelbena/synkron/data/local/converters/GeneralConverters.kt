package com.manuelbena.synkron.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manuelbena.synkron.domain.models.SubTaskDomain // Asegúrate de que este import sea correcto para tus Tareas normales

class StringListConverter {
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return Gson().toJson(list)
    }
}

class IntListConverter {
    @TypeConverter
    fun fromString(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromList(list: List<Int>?): String {
        return Gson().toJson(list)
    }
}

class SubTaskConverter {
    // Este es para las TAREAS NORMALES (TaskEntity)
    // Convierte la lista de SubTaskDomain a JSON
    @TypeConverter
    fun fromString(value: String): List<SubTaskDomain> {
        val listType = object : TypeToken<List<SubTaskDomain>>() {}.type
        return Gson().fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun fromList(list: List<SubTaskDomain>?): String {
        return Gson().toJson(list)
    }
}