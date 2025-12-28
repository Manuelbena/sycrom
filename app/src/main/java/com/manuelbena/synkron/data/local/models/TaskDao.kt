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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Query("SELECT * FROM task_table WHERE date >= :start AND date <= :end")
    fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskEntity>>

    // Busca si existe alguna tarea (hour = -1) en esa fecha exacta (date),
    // EXCLUYENDO la tarea actual (id != currentId) para permitir editar la misma tarea.
    @Query("SELECT COUNT(*) FROM task_table WHERE date = :dateMillis AND hour = -1 AND id != :excludedId")
    suspend fun checkAllDayTaskExists(dateMillis: Long, excludedId: Int): Int

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM task_table WHERE parent_id = :parentId")
    suspend fun getTasksByParentId(parentId: String): List<TaskEntity>

    // Opción rápida para borrar (pero ojo, no cancela alarmas una por una)
    @Query("DELETE FROM task_table WHERE parent_id = :parentId")
    suspend fun deleteSeriesRaw(parentId: String)

    // En TaskDao.kt

}