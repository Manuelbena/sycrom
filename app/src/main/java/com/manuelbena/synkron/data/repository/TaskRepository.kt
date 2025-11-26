package com.manuelbena.synkron.data.repository

import android.content.Context
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
// --- CAMBIO AQUÍ: Importamos el modelo desde el paquete correcto (.models) ---

import com.manuelbena.synkron.data.remote.n8n.N8nApi
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatRequest
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse
import com.manuelbena.synkron.data.scheduler.AlarmScheduler
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val api: N8nApi
) : ITaskRepository {

    /**
     * Obtiene un Flow de tareas para una fecha específica.
     * Convierte el LocalDate en los Timestamps Long que Room espera.
     */
    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        // 1. Convertir LocalDate -> Long (inicio del día)
        val dayStart = date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 2. Calcular fin del día (inicio del día siguiente)
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 3. Llamar al DAO (que devuelve Flow) y mapear el resultado
        return taskDao.getTasksForDay(dayStart, dayEnd).map { entityList ->
            // Mapea la lista de TaskEntity a TaskDomain
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun insertTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task.toEntity())
        alarmScheduler.schedule(task)
    }

    override suspend fun updateTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.toEntity())
        alarmScheduler.schedule(task)
    }

    override suspend fun deleteTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun sendIaMessage(message: String): Result<N8nChatResponse> {
        return try {
            val response = api.sendChatMessage(N8nChatRequest(message))

            if (response.isSuccessful && response.body() != null) {
                // Éxito: Devolvemos el cuerpo parseado
                Result.success(response.body()!!)
            } else {
                // Error del servidor (4xx, 5xx)
                Result.failure(Exception("Error en n8n: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            // Error de red o parsing
            Result.failure(e)
        }
    }
}