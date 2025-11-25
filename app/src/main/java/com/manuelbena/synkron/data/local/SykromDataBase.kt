package com.manuelbena.synkron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manuelbena.synkron.data.local.converters.RecurrenceTypeConverter
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.local.models.TaskEntity

@Database(entities = [TaskEntity::class], version = 3, exportSchema = false) // VERSIÓN 3
@TypeConverters(
    SubTaskConverter::class,
    StringListConverter::class,
    IntListConverter::class,
    RecurrenceTypeConverter::class
)
abstract class SykromDataBase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}