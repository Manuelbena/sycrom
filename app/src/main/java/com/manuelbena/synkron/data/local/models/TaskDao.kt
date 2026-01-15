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

    // Buscar tarea por el ID de Google
    @Query("SELECT * FROM task_table WHERE googleCalendarId = :googleId LIMIT 1")
    suspend fun getTaskByGoogleId(googleId: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM task_table WHERE date BETWEEN :start AND :end")
    suspend fun getCountTasksBetween(start: Long, end: Long): Int

    // Buscamos tareas que coincidan en NOMBRE y FECHA (Rango del día completo)
    // Y que NO tengan ya un ID real de Google (es decir, que sean NULL o FANTASMAS)
// En TaskDao.kt

    @Query("""
        SELECT * FROM task_table 
        WHERE summary = :summary 
        AND date >= :startOfDay AND date <= :endOfDay
        -- ELIMINAMOS la restricción de ID para permitir el re-enlace inteligente
        -- Si coincide nombre y fecha, asumimos que es la misma tarea y la actualizamos
        LIMIT 1
    """)
    suspend fun findLocalCandidate(summary: String, startOfDay: Long, endOfDay: Long): TaskEntity?

    // --- [NUEVO] OBTENER TODAS LAS TAREAS (Para el Centro de Gestión) ---
    @Query("SELECT * FROM task_table ORDER BY date DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    // En TaskDao.kt
    @Query("SELECT * FROM task_table WHERE googleCalendarId IS NULL OR googleCalendarId = ''")
    suspend fun getTasksWithoutGoogleId(): List<TaskEntity>

    // BORRADO DE HUÉRFANOS (Limpieza) 🧹
    // Borra las tareas que:
    // 1. Están en el rango de fechas que acabamos de sincronizar.
    // 2. Tienen un ID de Google (es decir, ya estaban sincronizadas antes).
    // 3. SU ID NO ESTÁ en la lista de IDs válidos que acabamos de recibir.
    // 4. (Opcional) No borramos 'LOCAL_GHOST' ni NULL por seguridad, aunque NULL no entra por la condición 2.

    // [NUEVO] Obtener lista para procesar en memoria (Más fiable que findLocalCandidate)
    @Query("SELECT * FROM task_table WHERE date >= :start AND date <= :end")
    suspend fun getTasksListForRange(start: Long, end: Long): List<TaskEntity>

    // Borra tareas que tienen ID de Google pero NO están en la lista "validIds" (Huérfanas)
    @Query("DELETE FROM task_table WHERE date >= :start AND date <= :end AND googleCalendarId IS NOT NULL AND googleCalendarId NOT IN (:validIds)")
    suspend fun deleteOrphanedTasks(start: Long, end: Long, validIds: List<String>)

    // Borra TODAS las tareas de Google en un rango (cuando Google devuelve lista vacía)
    @Query("DELETE FROM task_table WHERE date >= :start AND date <= :end AND googleCalendarId IS NOT NULL")
    suspend fun deleteAllSyncedTasksInRange(start: Long, end: Long)

}