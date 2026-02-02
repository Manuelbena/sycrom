package com.manuelbena.synkron.data.local.models

import androidx.room.*
import com.manuelbena.synkron.data.local.entities.SuperTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuperTaskDao {

    // Insertar o actualizar
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuperTask(task: SuperTaskEntity)

    // Obtener super tareas de un día específico
    // Buscamos entre el inicio y el fin de ese día
    @Query("SELECT * FROM super_tasks WHERE date >= :startOfDay AND date < :endOfDay")
    fun getSuperTasksForDate(startOfDay: Long, endOfDay: Long): Flow<List<SuperTaskEntity>>

    @Delete
    suspend fun deleteSuperTask(task: SuperTaskEntity)
}