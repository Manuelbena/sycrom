package com.manuelbena.synkron.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * TypeConverter para que Room pueda almacenar List<String>.
 * (Usado para 'attendeesEmails')
 */
class StringListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>): String {
        return gson.toJson(list)
    }
}

/**
 * TypeConverter para que Room pueda almacenar List<Int>.
 * (Usado para 'reminderMinutes')
 */
class IntListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<Int> {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<Int>): String {
        return gson.toJson(list)
    }
}