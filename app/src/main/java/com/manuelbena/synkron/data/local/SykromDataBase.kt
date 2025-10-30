package com.manuelbena.synkron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.local.models.EventDao

/**
 * La clase principal de la base de datos Room.
 * Define las entidades (tablas) y la versión de la BD.
 */
@Database(
    entities = [TaskDao::class],
    version = 2, // Versión incrementada por añadir el TypeConverter y nuevos campos
    exportSchema = false
)
@TypeConverters(SubTaskConverter::class) // <-- IMPORTANTE: Registrar el conversor
abstract class SykromDataBase : RoomDatabase() {

    // Room implementará esta función por nosotros.
    abstract fun eventDao(): EventDao

    // No necesitamos un Singleton (patrón getInstance) aquí
    // porque Hilt se encargará de gestionar la instancia única.
}
