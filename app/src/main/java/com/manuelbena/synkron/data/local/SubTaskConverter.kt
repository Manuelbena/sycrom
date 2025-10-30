package com.manuelbena.synkron.data.local


import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.manuelbena.synkron.domain.models.SubTaskDomain // <-- APUNTA A DOMAIN

/**
 * Le dice a Room cómo convertir la lista de SubTasks a un String (JSON)
 * y viceversa, para que pueda ser almacenada en una sola columna.
 */
class SubTaskConverter {

    private val gson = Gson()

    /**
     * Convierte una Lista de SubTask a un String JSON.
     */
    @TypeConverter
    fun fromSubTaskList(subTasks: List<SubTaskDomain>?): String? {
        if (subTasks == null) {
            return null
        }
        val type = object : TypeToken<List<SubTaskDomain>>() {}.type
        return gson.toJson(subTasks, type)
    }

    /**
     * Convierte un String JSON de vuelta a una Lista de SubTask.
     */
    @TypeConverter
    fun toSubTaskList(subTasksString: String?): List<SubTaskDomain>? {
        if (subTasksString == null) {
            return null
        }
        val type = object : TypeToken<List<SubTaskDomain>>() {}.type
        return gson.fromJson(subTasksString, type)
    }
}

