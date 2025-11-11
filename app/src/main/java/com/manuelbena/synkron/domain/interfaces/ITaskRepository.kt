package com.manuelbena.synkron.domain.interfaces



import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Interfaz (Contrato) para el repositorio de Tareas.
 * Define las operaciones que el Dominio (UseCases) puede realizar.
 */
interface ITaskRepository {

    /**
     * Inserta una nueva tarea.
     */
    suspend fun insertTask(task: TaskDomain)

    /**
     * Actualiza una tarea existente.
     */
    suspend fun updateTask(task: TaskDomain)

    /**
     * Obtiene un Flow (flujo reactivo) de tareas para una fecha específica.
     */
    fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>>

    /**
     * Elimina una tarea. (¡Añadido!)
     */
    suspend fun deleteTask(task: TaskDomain)
}