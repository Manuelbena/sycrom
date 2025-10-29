package com.manuelbena.synkron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.local.models.EventDao

/**
 * La clase principal de la base de datos Room.
 * Define las entidades (tablas) y la versión de la BD.
 *
 * exportSchema = false se usa para este ejemplo para evitar un
 * warning de compilación. En producción, es buena idea gestionarlos.
 */
@Database(
    entities = [TaskDao::class],
    version = 1,
    exportSchema = false
)
abstract class SykromDataBase : RoomDatabase() {

    // Room implementará esta función por nosotros.
    abstract fun eventDao(): EventDao

    // No necesitamos un Singleton (patrón getInstance) aquí
    // porque Hilt se encargará de gestionar la instancia única.
}
