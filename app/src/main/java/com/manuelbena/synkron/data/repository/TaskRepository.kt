package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.local.models.EventDao
import com.manuelbena.synkron.data.mappers.toData
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val dao: EventDao
) : ITaskRepository {

    // --- IMPLEMENTACIÓN MODIFICADA ---
    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        // 1. Definir la zona horaria del dispositivo
        val zoneId = ZoneId.systemDefault()

        // 2. Calcular el timestamp de inicio de ese día (00:00:00)
        val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()

        // 3. Calcular el timestamp de inicio del DÍA SIGUIENTE (00:00:00)
        val nextDayStartTime = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        // 4. Llamar al DAO con los timestamps y mapear a Dominio
        return dao.getTasksForDateRange(startOfDay, nextDayStartTime).map { taskDaoList ->
            taskDaoList.map { it.toDomain() }
        }
    }
    // --- FIN MODIFICACIÓN ---

    override suspend fun insertEvent(task: TaskDomain) {
        dao.insertEvent(task.toData())
    }

    override suspend fun updateEvent(task: TaskDomain) {
        dao.updateEvent(task.toData())
    }
}
