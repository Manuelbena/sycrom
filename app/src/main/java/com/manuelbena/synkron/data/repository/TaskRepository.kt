package com.manuelbena.synkron.data.repository

import android.util.Log
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.data.mappers.toTaskDomain
import com.manuelbena.synkron.data.remote.n8n.N8nApi
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatRequest
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse
import com.manuelbena.synkron.data.scheduler.AlarmScheduler
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val api: N8nApi
) : ITaskRepository {

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        // 1. Calculamos el inicio del día (00:00:00)
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 2. Calculamos el final del día (23:59:59.999...)
        val endOfDay = date.atTime(LocalTime.MAX)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // 3. Pedimos al DAO solo ese rango exacto
        return taskDao.getTasksBetween(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskDomain>> {
        return taskDao.getTasksBetweenDates(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 🔥 CORRECCIÓN AQUÍ: Quitamos el "=" y usamos llaves para el cuerpo
    override suspend fun insertTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Insertamos y guardamos el ID
                val newId = taskDao.insertTask(task.toEntity())
                Log.d("SYCROM_DEBUG", "REPO: Tarea insertada correctamente. ID generado: $newId")

                // 2. Actualizamos la tarea con el ID real para la alarma
                val taskWithId = task.copy(id = newId.toInt())

                // 3. Programamos
                alarmScheduler.schedule(taskWithId)
            } catch (e: Exception) {
                Log.e("SYCROM_DEBUG", "REPO ERROR: Fallo al insertar tarea: ${e.message}")
                throw e
            }
        }
    }
    override suspend fun hasAllDayTaskOnDate(date: LocalDate, excludedId: Int): Boolean {
        // Generamos el timestamp exactamente igual que en el Mapper de guardado (DomainToData)
        // Inicio del día en la zona horaria del sistema.
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Consultamos al DAO
        val count = taskDao.checkAllDayTaskExists(millis, excludedId)

        return count > 0 // True si ya existe alguna
    }
    override suspend fun getTaskById(id: Int): TaskDomain? = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(id)
        entity?.toDomain()
    }


    override suspend fun updateTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task.toEntity())
        Log.d("SYCROM_DEBUG", "REPO: Tarea actualizada. ID: ${task.id}")
        alarmScheduler.schedule(task)
    }

    override suspend fun deleteTask(task: TaskDomain) = withContext(Dispatchers.IO) {
        alarmScheduler.cancel(task)
        taskDao.deleteTask(task.toEntity())
    }

    // --- AQUÍ ESTÁ LA MAGIA ---
    override suspend fun sendIaMessage(message: String): Result<TaskDomain> {
        return try {
            // 1. Llamada a la API (devuelve Response<N8nChatResponse>)
            val response = api.sendChatMessage(N8nChatRequest(message))

            if (response.isSuccessful && response.body() != null) {
                // 2. Obtenemos el DTO crudo
                val n8nResponse = response.body()!!

                // 3. CONVERSIÓN: Usamos el mapper para transformarlo a Domain
                val domainTask = n8nResponse.toTaskDomain()

                // 4. Devolvemos éxito con el objeto de Dominio
                Result.success(domainTask)
            } else {
                Result.failure(Exception("Error en n8n: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}