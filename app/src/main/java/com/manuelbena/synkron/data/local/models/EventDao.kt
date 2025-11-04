package com.manuelbena.synkron.data.local.models

// ... (tus imports)
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // --- MODIFICADO ---
    /**
     * Busca tareas donde 'date' (timestamp) sea:
     * - Mayor o IGUAL a [startTime] (p.ej., 4 de Nov a las 00:00:00)
     * - MENOR que [nextDayStartTime] (p.ej., 5 de Nov a las 00:00:00)
     */
    @Query("SELECT * FROM events_table WHERE start_time >= :startTime AND start_time < :nextDayStartTime ORDER BY hour ASC")
    fun getTasksForDateRange(startTime: Long, nextDayStartTime: Long): Flow<List<TaskDao>>
    // --- FIN MODIFICADO ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(task: TaskDao)

    @Update
    suspend fun updateEvent(task: TaskDao)
}
