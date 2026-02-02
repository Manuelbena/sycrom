package com.manuelbena.synkron.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.manuelbena.synkron.data.local.converters.IntListConverter
import com.manuelbena.synkron.data.local.converters.RecurrenceTypeConverter
import com.manuelbena.synkron.data.local.converters.StringListConverter
import com.manuelbena.synkron.data.local.converters.SubTaskConverter
import com.manuelbena.synkron.data.local.converters.SuperTaskConverters // <--- IMPORTANTE: Asegúrate de tener este archivo creado
import com.manuelbena.synkron.data.local.models.SuperTaskDao
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.local.models.TaskEntity
import com.manuelbena.synkron.data.local.entities.SuperTaskEntity // <--- IMPORTANTE: Importa tu nueva entidad

@Database(
    entities = [
        TaskEntity::class,
        SuperTaskEntity::class // 1. AÑADIMOS LA NUEVA TABLA AQUÍ
    ],
    version = 5, // 2. SUBIMOS LA VERSIÓN (Para forzar actualización)
    exportSchema = false
)
@TypeConverters(
    SubTaskConverter::class,
    StringListConverter::class,
    IntListConverter::class,
    RecurrenceTypeConverter::class,
    SuperTaskConverters::class // 3. AÑADIMOS EL CONVERTIDOR PARA LA LISTA DE SUBTAREAS
)
abstract class SykromDataBase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun superTaskDao(): SuperTaskDao
}