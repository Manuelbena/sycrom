package com.manuelbena.synkron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.local.models.EventDao
import com.manuelbena.synkron.data.local.models.TaskEntity

/**
 * La clase principal de la base de datos Room.
 * Define las entidades (tablas) y la versión de la BD.
 */
@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
@TypeConverters(
    SubTaskConverter::class,
    StringListConverter::class,
    IntListConverter::class
)
abstract class SykromDataBase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}