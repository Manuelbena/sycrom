package com.manuelbena.synkron.data.local.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // CORRECCIÓN: Usamos 'date' que es el nombre real de la columna en tu TaskEntity
    @Query("SELECT * FROM task_table WHERE date BETWEEN :dayStart AND :dayEnd ORDER BY date ASC")
    fun getTasksForDay(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    // IMPORTANTE: Debe devolver Long para obtener el ID de la nueva fila
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}