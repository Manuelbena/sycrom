package com.manuelbena.synkron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.local.models.TaskEntity // ¡CAMBIO!

// CAMBIO: La entidad ahora es 'TaskEntity'
@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
// CAMBIO: Añadimos los nuevos conversores
@TypeConverters(
    SubTaskConverter::class,
    StringListConverter::class,
    IntListConverter::class,
    RemindersConverter::class
)
abstract class SykromDataBase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}