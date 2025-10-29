package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.local.models.EventDao
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toData
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.first // <-- 1. IMPORTA la función 'first'
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor(
    private val eventDao: EventDao
) : ITaskRepository {

    /**
     * Obtiene la lista de tareas de forma asíncrona.
     * Utiliza .first() para recolectar el primer valor emitido por el Flow de Room.
     */
    override suspend fun getTaskToday(): List<TaskDomain> {
        // 2. RECOLECTA el primer valor del Flow (que es la lista)
        val taskDaoList: List<TaskDao> = eventDao.getAllEvents().first()

        // 3. MAPEA la lista de DAOs a una lista de objetos de Dominio y la devuelve
        return taskDaoList.map { taskDao ->
            taskDao.toDomain()
        }
    }

    /**
     * Inserta una nueva tarea en la base de datos.
     */
    override suspend fun insertEvent(taskDomain: TaskDomain) {
        eventDao.insertEvent(taskDomain.toData())
    }

    /**
     * Actualiza una tarea existente.
     * TODO: Implementar la lógica de actualización.
     */
    override suspend fun updateEvent(taskDao: TaskDomain) {
        // Lógica futura: eventDao.updateEvent(taskDao.toData())
        TODO("Not yet implemented")
    }

    /**
     * Elimina una tarea de la base de datos.
     */
    override suspend fun deleteEvent(taskDomain: TaskDomain) {
        eventDao.deleteEvent(taskDomain.toData())
    }
}
