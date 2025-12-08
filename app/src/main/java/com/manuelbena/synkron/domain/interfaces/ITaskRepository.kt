package com.manuelbena.synkron.domain.interfaces



import com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse
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

    /**
     * Promnt que se le pasa al n8n para que me de una tarea
     */
    suspend fun sendIaMessage(message: String): Result<TaskDomain>

    suspend fun getTaskById(id: Int): TaskDomain?

     fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskDomain>>


}