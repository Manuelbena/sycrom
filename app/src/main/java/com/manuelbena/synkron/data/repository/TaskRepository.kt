package com.manuelbena.synkron.data.repository


import com.manuelbena.synkron.data.local.models.EventDao
import kotlinx.coroutines.flow.map
import com.manuelbena.synkron.data.mappers.toData
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

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
     */
    override suspend fun updateEvent(taskDomain: TaskDomain) {
        // Lógica implementada:
        eventDao.updateEvent(taskDomain.toData())
    }

    /**
     * Elimina una tarea de la base de datos.
     */
    override suspend fun deleteEvent(taskDomain: TaskDomain) {
        eventDao.deleteEvent(taskDomain.toData())
    }

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        // 1. Definir la zona horaria del dispositivo
        val zoneId = ZoneId.systemDefault()

        // 2. Calcular el timestamp de inicio de ese día (00:00:00)
        val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // 3. Calcular el timestamp de inicio del DÍA SIGUIENTE (00:00:00)
        val nextDayStartTime = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        // 4. Llamar al DAO con los timestamps y mapear a Dominio
        return eventDao.getTasksForDateRange(startOfDay, nextDayStartTime).map { taskDaoList ->
            taskDaoList.map { it.toDomain() } // Asumiendo que tienes tu mapper
        }
    }
}
