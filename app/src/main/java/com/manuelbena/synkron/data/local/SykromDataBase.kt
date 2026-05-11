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
import com.manuelbena.synkron.data.local.models.BudgetDao
import com.manuelbena.synkron.data.local.models.BudgetEntity
import com.manuelbena.synkron.data.local.models.TransactionEntity

@Database(
    entities = [
        TaskEntity::class,
        SuperTaskEntity::class,
        BudgetEntity::class,
        TransactionEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(
    SubTaskConverter::class,
    StringListConverter::class,
    IntListConverter::class,
    RecurrenceTypeConverter::class,
    SuperTaskConverters::class ,
)
abstract class SykromDataBase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun superTaskDao(): SuperTaskDao
    abstract fun budgetDao(): BudgetDao
}