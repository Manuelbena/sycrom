package com.manuelbena.synkron.data.repository


import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.data.remote.n8n.IngestRequest
import com.manuelbena.synkron.data.remote.n8n.N8nApi
import com.manuelbena.synkron.data.worker.N8nIngestWorker
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
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    @ApplicationContext private val context: Context,
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
    }

    override suspend fun updateTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: TaskDomain) = withContext(Dispatchers.IO) {
         taskDao.deleteTask(task.toEntity())
    }

    override suspend fun sendTaskToAi(message: String): Result<Boolean> {
        return try {
            val request = IngestRequest(
                idempotencyKey = UUID.randomUUID().toString(),
                message = message
            )
            val response = api.sendEvent(request)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}