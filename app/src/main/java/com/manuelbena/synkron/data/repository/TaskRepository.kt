package com.manuelbena.synkron.data.repository

import android.util.Log
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.data.mappers.toTaskDomain
import com.manuelbena.synkron.data.remote.n8n.N8nApi
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatRequest
import com.manuelbena.synkron.data.scheduler.AlarmScheduler
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val api: N8nApi
) : ITaskRepository {

    // ----------------------------------------------------------------
    // LECTURA DE DATOS
    // ----------------------------------------------------------------

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return taskDao.getTasksBetween(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskDomain>> {
        return taskDao.getTasksBetweenDates(start, end).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTaskById(id: Int): TaskDomain? = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(id)
        entity?.toDomain()
    }

    override suspend fun hasAllDayTaskOnDate(date: LocalDate, excludedId: Int): Boolean {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val count = taskDao.checkAllDayTaskExists(millis, excludedId)
        return count > 0
    }

    // ----------------------------------------------------------------
    // CREACIÓN (INSERCIÓN) CON LOGICA RECURSIVA
    // ----------------------------------------------------------------

    // ----------------------------------------------------------------
    // CREACIÓN (INSERCIÓN) CON LOGS DE DEPURACIÓN
    // ----------------------------------------------------------------

    override suspend fun insertTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            try {
                // 1. INSPECCIÓN DE ENTRADA
                Log.e("SYCROM_RECURRENCIA", "🟢 INTENTO DE INSERTAR: ${task.summary}")
                Log.e("SYCROM_RECURRENCIA", "📅 Días seleccionados (Raw): ${task.synkronRecurrenceDays}")

                val isRecurring = task.synkronRecurrenceDays.isNotEmpty()

                if (isRecurring) {
                    Log.e("SYCROM_RECURRENCIA", "👉 Detectado como RECURSIVO viaja a insertRecursiveSeries")
                    insertRecursiveSeries(task)
                } else {
                    Log.e("SYCROM_RECURRENCIA", "👉 Detectado como ÚNICO viaja a insertSingleTask")
                    insertSingleTask(task, parentId = null)
                }
            } catch (e: Exception) {
                Log.e("SYCROM_RECURRENCIA", "🔴 CRASH EN INSERTAR: ${e.message}")
                e.printStackTrace()
                throw e
            }
        }
    }

    private suspend fun insertRecursiveSeries(originalTask: TaskDomain) {
        val batchId = UUID.randomUUID().toString()
        val startDate = originalTask.start?.toLocalDate() ?: LocalDate.now()
        val endDate = startDate.plusYears(1)

        Log.e("SYCROM_RECURRENCIA", "🔄 Iniciando bucle de generación:")
        Log.e("SYCROM_RECURRENCIA", "   - Desde: $startDate")
        Log.e("SYCROM_RECURRENCIA", "   - Hasta: $endDate")
        Log.e("SYCROM_RECURRENCIA", "   - Días buscados: ${originalTask.synkronRecurrenceDays}")

        var currentDate = startDate
        var createdCount = 0

        while (currentDate.isBefore(endDate)) {
            // java.time.DayOfWeek devuelve: 1 (Lunes) ... 7 (Domingo)
            val dayOfWeek = currentDate.dayOfWeek.value

            // Logueamos solo los primeros 7 días para no saturar el log
            if (createdCount < 5 && currentDate.isBefore(startDate.plusDays(8))) {
                Log.d("SYCROM_RECURRENCIA", "   🔎 Revisando $currentDate es día nro: $dayOfWeek. ¿Está en la lista? ${originalTask.synkronRecurrenceDays.contains(dayOfWeek)}")
            }

            if (originalTask.synkronRecurrenceDays.contains(dayOfWeek)) {
                val newTask = originalTask.copy(
                    id = 0,
                    start = originalTask.start?.copyWithNewDate(currentDate),
                    end = originalTask.end?.copyWithNewDate(currentDate)
                )
                insertSingleTask(newTask, batchId)
                createdCount++
            }
            currentDate = currentDate.plusDays(1)
        }

        Log.e("SYCROM_RECURRENCIA", "✅ Bucle terminado. Total tareas creadas: $createdCount")
    }

    /**
     * Lógica privada para insertar UNA sola tarea.
     * Se encarga de mapear, guardar en BD y programar la alarma.
     */
    private suspend fun insertSingleTask(task: TaskDomain, parentId: String?) {
        // 1. Convertimos a entidad y asignamos el ParentID (si existe)
        // Nota: Asegúrate de que toEntity() mapee el parentId o haz el copy aquí:
        val entity = task.toEntity().copy(parentId = parentId)

        // 2. Insertamos en BD
        val newId = taskDao.insertTask(entity)
        Log.d("SYCROM_DEBUG", "REPO: Tarea insertada ID: $newId, Parent: $parentId")

        // 3. Optimizacion: Solo programar alarma si es en los próximos 7 días
        // para no saturar el AlarmManager con eventos de dentro de 6 meses.
        val taskDate = task.start?.toLocalDate() ?: LocalDate.now()
        val limitDateForAlarm = LocalDate.now().plusDays(7)

        if (taskDate.isBefore(limitDateForAlarm) || taskDate.isEqual(limitDateForAlarm)) {
            // Reconstruimos el objeto con el ID generado para que el Scheduler funcione
            val taskWithId = task.copy(id = newId.toInt(), parentId = parentId)
            alarmScheduler.schedule(taskWithId)
        }
    }


    // ----------------------------------------------------------------
    // ACTUALIZACIÓN
    // ----------------------------------------------------------------

    override suspend fun updateTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Recuperar la tarea ANTIGUA para cancelar alarmas previas
                val oldTask = taskDao.getTaskById(task.id)?.toDomain()
                if (oldTask != null) {
                    alarmScheduler.cancel(oldTask)
                }

                // 2. Guardar la NUEVA versión
                taskDao.updateTask(task.toEntity())
                Log.d("SYCROM_DEBUG", "REPO: Tarea actualizada ID: ${task.id}")

                // 3. Programar las NUEVAS alarmas
                alarmScheduler.schedule(task)

            } catch (e: Exception) {
                Log.e("SYCROM_DEBUG", "REPO: Error actualizando: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------------------
    // BORRADO (Instance vs Series)
    // ----------------------------------------------------------------

    // Opción 1: Borrar solo este día
    override suspend fun deleteTaskInstance(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            alarmScheduler.cancel(task)
            taskDao.deleteTask(task.toEntity())
            Log.d("SYCROM_DEBUG", "Instancia borrada: ${task.id}")
        }
    }

    // Opción 2: Borrar toda la serie
    override suspend fun deleteTaskSeries(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            val pId = task.parentId

            if (pId.isNullOrEmpty()) {
                // No es serie, borramos normal
                deleteTaskInstance(task)
            } else {
                // 1. Buscamos todas las tareas hermanas en BD
                // REQUISITO: Tener getTasksByParentId en TaskDao
                val relatedTasks = taskDao.getTasksByParentId(pId)

                // 2. Cancelamos alarmas y borramos una por una
                relatedTasks.forEach { entity ->
                    val domain = entity.toDomain()
                    alarmScheduler.cancel(domain)
                    taskDao.deleteTask(entity)
                }
                Log.d("SYCROM_DEBUG", "Serie borrada. ParentID: $pId. Total: ${relatedTasks.size}")
            }
        }
    }

    // Por defecto, redirige a borrar instancia (Legacy support)
    override suspend fun deleteTask(task: TaskDomain) {
        deleteTaskInstance(task)
    }

    // ----------------------------------------------------------------
    // INTELIGENCIA ARTIFICIAL (N8N)
    // ----------------------------------------------------------------

    override suspend fun sendIaMessage(message: String): Result<TaskDomain> {
        return try {
            val response = api.sendChatMessage(N8nChatRequest(message))
            if (response.isSuccessful && response.body() != null) {
                val n8nResponse = response.body()!!
                val domainTask = n8nResponse.toTaskDomain()
                Result.success(domainTask)
            } else {
                Result.failure(Exception("Error en n8n: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------------------
    // EXTENSIONES / HELPERS PRIVADOS
    // ----------------------------------------------------------------

    /**
     * Función de ayuda para cambiar la fecha de un GoogleEventDateTime
     * manteniendo la hora original (si existe) o el formato "todo el día".
     */
    private fun GoogleEventDateTime.copyWithNewDate(newDate: LocalDate): GoogleEventDateTime {
        return if (this.dateTime != null) {
            // Caso 1: Tiene hora específica -> Mantenemos hora, cambiamos día
            val originalTime = java.time.Instant.ofEpochMilli(this.dateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()

            val newDateTimeMillis = LocalDateTime.of(newDate, originalTime)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            this.copy(dateTime = newDateTimeMillis, date = null)
        } else {
            // Caso 2: Es todo el día -> Solo cambiamos el string YYYY-MM-DD
            this.copy(date = newDate.toString(), dateTime = null)
        }
    }
}