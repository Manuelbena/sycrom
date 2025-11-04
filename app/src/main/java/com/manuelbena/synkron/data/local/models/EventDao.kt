package com.manuelbena.synkron.data.local.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para la entidad Event.
 * Aquí defines todas las consultas a la base de datos.
 * Usamos 'suspend' para operaciones de una sola vez (insert, update, delete)
 * y 'Flow' para consultas que deben reaccionar a cambios (observar).
 */
@Dao
interface EventDao {

    /**
     * Inserta un evento. Si el evento ya existe (misma ID),
     * lo reemplaza. Esto es útil para la función "guardar".
     * @param taskDao El objeto evento a insertar o reemplazar.
     * @return El ID del evento insertado (Long).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(taskDao: TaskDao): Long

    /**
     * Actualiza un evento existente.
     */
    @Update
    suspend fun updateEvent(taskDao: TaskDao)

    /**
     * Borra un evento.
     */
    @Delete
    suspend fun deleteEvent(taskDao: TaskDao)

    /**
     * Obtiene un evento específico por su ID.
     * @param eventId El ID del evento a buscar.
     * @return Un Flow que emite el Evento (o null si no se encuentra).
     */
    @Query("SELECT * FROM events_table WHERE id = :eventId")
    fun getEventById(eventId: Long): Flow<TaskDao?>

    /**
     * Obtiene todos los eventos ordenados por fecha de inicio.
     * @return Un Flow que emite la lista completa de eventos.
     */
    @Query("SELECT * FROM events_table ORDER BY start_time ASC")
    fun getAllEvents(): Flow<List<TaskDao>>

    /**
     * Obtiene todos los eventos dentro de un rango de fechas (ej. para la vista de día o semana).
     * @param startTime El timestamp de inicio del rango.
     * @param endTime El timestamp de fin del rango.
     * @return Un Flow con la lista de eventos en ese rango.
     */
    @Query("SELECT * FROM events_table WHERE start_time BETWEEN :startTime AND :endTime ORDER BY start_time ASC")
    fun getEventsInRange(startTime: Long, endTime: Long): Flow<List<TaskDao>>

    /**
     * Nueva consulta para obtener tareas por rango de fechas.
     * Busca tareas donde 'date' (timestamp) sea:
     * - Mayor o IGUAL a [startTime] (p.ej., 4 de Nov a las 00:00:00)
     * - MENOR que [nextDayStartTime] (p.ej., 5 de Nov a las 00:00:00)
     */
    @Query("SELECT * FROM events_table WHERE start_time >= :startTime AND start_time < :nextDayStartTime ORDER BY hour ASC")
    fun getTasksForDateRange(startTime: Long, nextDayStartTime: Long): Flow<List<TaskDao>>
}