package com.manuelbena.synkron.data.repository

import android.content.Context
import android.util.Log
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
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
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val api: N8nApi
) : ITaskRepository {

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        val dayStart = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return taskDao.getTasksForDay(dayStart, dayEnd).map { entityList ->
            entityList.map { it.toDomain() }
        }
    }

    override suspend fun insertTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        // 1. Insertamos y OBTENEMOS el ID generado (ej: 154)
        val newId = taskDao.insertTask(task.toEntity())

        Log.d("SYCROM_DEBUG", "REPO: Tarea insertada. ID generado por BD: $newId")

        // 2. Creamos una copia de la tarea con el ID REAL para el Scheduler
        val taskWithId = task.copy(id = newId.toInt())

        // 3. Programamos la alarma con el ID correcto
        alarmScheduler.schedule(taskWithId)
    }

    override suspend fun updateTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.toEntity())
        Log.d("SYCROM_DEBUG", "REPO: Tarea actualizada. ID: ${task.id}")
        alarmScheduler.schedule(task)
    }

    override suspend fun deleteTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        alarmScheduler.cancel(task) // Es buena práctica cancelar la alarma al borrar
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun sendIaMessage(message: String): Result<N8nChatResponse> {
        return try {
            val response = api.sendChatMessage(N8nChatRequest(message))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Error en n8n: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}