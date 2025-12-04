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

    // CORRECCIÓN 1: Cambiado 'start_date' por 'date' (el nombre real en tu TaskEntity)
    @Query("SELECT * FROM task_table WHERE date BETWEEN :dayStart AND :dayEnd ORDER BY date ASC")
    fun getTasksForDay(dayStart: Long, dayEnd: Long): Flow<List<TaskEntity>>

    // --- NUEVO: Obtener una tarea concreta por ID ---
    @Query("SELECT * FROM task_table WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): TaskEntity?

    // ✅ AHORA (Corregido con rango de inicio y fin):
    @Query("SELECT * FROM task_table WHERE date BETWEEN :startOfDay AND :endOfDay ORDER BY date ASC")
    fun getTasksBetween(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    // CORRECCIÓN 2: Añadido ': Long' para devolver el ID generado
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)
}