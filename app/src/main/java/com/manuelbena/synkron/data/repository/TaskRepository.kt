package com.manuelbena.synkron.data.repository


import com.manuelbena.synkron.data.local.models.EventDao
import kotlinx.coroutines.flow.map
import com.manuelbena.synkron.data.mappers.toData
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain

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
    override fun getTaskToday(): kotlinx.coroutines.flow.Flow<List<TaskDomain>> {

        return eventDao.getAllEvents().map { eventDaoList ->
            eventDaoList.map { eventDao ->
                eventDao.toDomain()
            }
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
