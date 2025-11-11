package com.manuelbena.synkron.data.local.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // ¡Importar Flow!

@Dao
interface TaskDao {

    // CAMBIO: Opera sobre TaskEntity
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    // CAMBIO: Opera sobre TaskEntity
    @Update
    suspend fun updateTask(task: TaskEntity)

    // --- ¡CAMBIO CRÍTICO! ---
    // Ya no es 'suspend' y devuelve 'Flow<List<TaskEntity>>'
    @Query("SELECT * FROM task_table WHERE date >= :dayStart AND date < :dayEnd")
    fun getTasksForDay(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    // (Añade también el delete si lo necesitas)
    // @Delete
    // suspend fun deleteTask(task: TaskEntity)
}