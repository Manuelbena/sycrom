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
    private val api: N8nApi,
    private val googleCalendarRepository: GoogleCalendarRepository
) : ITaskRepository {

    // ----------------------------------------------------------------
    // LECTURA
    // ----------------------------------------------------------------

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return taskDao.getTasksBetween(startOfDay, endOfDay).map { list ->
            list.map { it.toDomain() }
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
    // 🧠 LOGICA DE SINCRONIZACIÓN INTELIGENTE
    // ----------------------------------------------------------------

    /**
     * Sincronización completa manual (Botón Sync)
     */
    suspend fun synchronizeWithGoogle() = withContext(Dispatchers.IO) {
        Log.d("SycromSync", "🔄 Forzando sincronización completa...")
        val (now, rangeEnd) = getSyncRange()

        // Bajamos todo de Google
        val googleTasks = googleCalendarRepository.fetchEventsBetween(now, rangeEnd)

        // Procesamos sin preguntar (Fuerza bruta para asegurar consistencia)
        processGoogleList(googleTasks)
    }

    /**
     * Sincronización "Smart" (Se llama al insertar)
     * Compara cantidades antes de tocar la base de datos local.
     */
    private suspend fun checkAndSmartSync() {
        try {
            val (now, rangeEnd) = getSyncRange()

            // 1. Obtenemos la lista de Google (Necesaria para saber su longitud real)
            val googleTasks = googleCalendarRepository.fetchEventsBetween(now, rangeEnd)
            val googleCount = googleTasks.size

            // 2. Consultamos la cantidad en Local (Muy rápido)
            val localCount = taskDao.getCountTasksBetween(now, rangeEnd)

            Log.d("SycromSync", "🧐 Comparando: Google($googleCount) vs Local($localCount)")

            // 3. LA REGLA DE ORO: Si son distintos, sincronizamos.
            if (googleCount != localCount) {
                Log.i("SycromSync", "⚠️ Descuadre detectado. Sincronizando BD Local...")
                processGoogleList(googleTasks) // Pasamos la lista que ya descargamos
            } else {
                Log.d("SycromSync", "✅ Todo cuadrado. Nos ahorramos procesar la BD.")
            }

        } catch (e: Exception) {
            Log.e("SycromSync", "Error en SmartSync: ${e.message}")
        }
    }

    /**
     * Lógica central de fusionado (Reutilizable)
     */
    private suspend fun processGoogleList(googleTasks: List<TaskDomain>) {
        googleTasks.forEach { googleTask ->
            val gId = googleTask.googleCalendarId ?: return@forEach

            // 1. Calcular la fecha correcta de la tarea de Google
            val googleDateMillis = calculateGoogleDateMillis(googleTask)

            // 2. Primero buscamos por ID EXACTO (La forma ideal)
            var localEntity = taskDao.getTaskByGoogleId(gId)

            // 3. ESTRATEGIA ANTI-DUPLICADOS:
            // Si no la encontramos por ID, buscamos si existe una tarea local
            // con el MISMO NOMBRE y en el MISMO DÍA.
            if (localEntity == null) {
                val startOfDay = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(googleDateMillis),
                    ZoneId.systemDefault()
                ).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                val endOfDay = LocalDate.ofInstant(
                    java.time.Instant.ofEpochMilli(googleDateMillis),
                    ZoneId.systemDefault()
                ).atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Buscamos la candidata
                localEntity = taskDao.findLocalCandidate(googleTask.summary, startOfDay, endOfDay)

                if (localEntity != null) {
                    Log.d("SycromSync", "🔗 ¡Fusión encontrada! Enlazando local '${localEntity.summary}' con Google.")
                }
            }

            if (localEntity != null) {
                // --- ACTUALIZAR / ENLAZAR ---
                // Al hacer el copy, le asignamos el googleCalendarId.
                // Así, la próxima vez ya se encontrarán por ID directo.
                val updatedEntity = localEntity.copy(
                    googleCalendarId = gId, // <--- AQUÍ OCURRE EL ENLACE
                    summary = googleTask.summary,
                    description = googleTask.description ?: localEntity.description,
                    date = googleDateMillis,
                    // Si la tarea local tenía hora 0 (sin definir) y google trae hora, actualizamos
                    hour = if (googleTask.start?.dateTime != null) {
                        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(googleTask.start.dateTime), ZoneId.systemDefault()).hour
                    } else localEntity.hour
                )
                taskDao.updateTask(updatedEntity)
            } else {
                // --- INSERTAR (Solo si de verdad no existe nada parecido) ---
                val newEntity = googleTask.toEntity().copy(
                    id = 0,
                    googleCalendarId = gId,
                    typeTask = "Personal",
                    priority = "Media",
                    date = googleDateMillis
                )
                taskDao.insertTask(newEntity)
            }
        }
    }

    // ----------------------------------------------------------------
    // INSERCIÓN (Con Smart Sync)
    // ----------------------------------------------------------------

    override suspend fun insertTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            // 1. Insertar en Google (Fuente de verdad)
            val googleId = googleCalendarRepository.insertEvent(task)

            // 2. Insertar en Local
            val taskToSave = task.copy(googleCalendarId = googleId)
            Log.e("SycromSync", "💾 Guardando insert local. G-ID: $googleId")

            if (task.synkronRecurrenceDays.isNotEmpty()) {
                insertRecursiveSeries(taskToSave)
            } else {
                insertSingleTask(taskToSave, null)
            }

            // 3. 🚀 DISPARAR VERIFICACIÓN POST-INSERCIÓN
            // Comprobamos si nos hemos quedado desalineados con la nube
            checkAndSmartSync()
        }
    }

    // Alias para compatibilidad
    suspend fun insert(task: TaskDomain) = insertTask(task)


    // ----------------------------------------------------------------
    // HELPERS Y OTROS MÉTODOS
    // ----------------------------------------------------------------

    private fun getSyncRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val rangeEnd = now + (30L * 24 * 60 * 60 * 1000) // Próximos 30 días
        return Pair(now, rangeEnd)
    }

    private fun calculateGoogleDateMillis(googleTask: TaskDomain): Long {
        return when {
            googleTask.start?.dateTime != null -> googleTask.start.dateTime
            googleTask.start?.date != null -> {
                try {
                    LocalDate.parse(googleTask.start.date)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant().toEpochMilli()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            }
            else -> System.currentTimeMillis()
        }
    }

    // ... Resto de métodos privados de inserción recursiva (insertRecursiveSeries, insertSingleTask) ...
    // ... Mantenlos igual que en tu código anterior ...
    private suspend fun insertRecursiveSeries(originalTask: TaskDomain) {
        val batchId = UUID.randomUUID().toString()
        val startDate = originalTask.start?.toLocalDate() ?: LocalDate.now()
        val endDate = startDate.plusYears(1)
        var currentDate = startDate
        while (currentDate.isBefore(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value
            if (originalTask.synkronRecurrenceDays.contains(dayOfWeek)) {
                val newTask = originalTask.copy(
                    id = 0,
                    start = originalTask.start?.copyWithNewDate(currentDate),
                    end = originalTask.end?.copyWithNewDate(currentDate)
                )
                insertSingleTask(newTask, batchId)
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    private suspend fun insertSingleTask(task: TaskDomain, parentId: String?) {
        val entity = task.toEntity().copy(parentId = parentId)
        val newId = taskDao.insertTask(entity)
        val taskDate = task.start?.toLocalDate() ?: LocalDate.now()
        val limitDateForAlarm = LocalDate.now().plusDays(7)
        if (taskDate.isBefore(limitDateForAlarm) || taskDate.isEqual(limitDateForAlarm)) {
            val taskWithId = task.copy(id = newId.toInt(), parentId = parentId)
            alarmScheduler.schedule(taskWithId)
        }
    }

    // ----------------------------------------------------------------
    // ACTUALIZACIÓN / BORRADO / N8N (Sin cambios, solo añadidos por completitud)
    // ----------------------------------------------------------------

    override suspend fun updateTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            val oldTask = taskDao.getTaskById(task.id)?.toDomain()
            if (oldTask != null) alarmScheduler.cancel(oldTask)
            taskDao.updateTask(task.toEntity())
            alarmScheduler.schedule(task)
        }
    }

    override suspend fun deleteTaskInstance(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            alarmScheduler.cancel(task)
            taskDao.deleteTask(task.toEntity())
        }
    }

    override suspend fun deleteTaskSeries(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            deleteTaskInstance(task) // Simplificado
        }
    }

    override suspend fun deleteTask(task: TaskDomain) = deleteTaskInstance(task)

    override suspend fun sendIaMessage(message: String): Result<TaskDomain> {
        return try {
            val response = api.sendChatMessage(N8nChatRequest(message))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toTaskDomain())
            } else {
                Result.failure(Exception("Error en n8n: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun GoogleEventDateTime.copyWithNewDate(newDate: LocalDate): GoogleEventDateTime {
        // (Tu implementación anterior del copyWithNewDate)
        return if (this.dateTime != null) {
            val originalTime = java.time.Instant.ofEpochMilli(this.dateTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
            val newDateTimeMillis = LocalDateTime.of(newDate, originalTime)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
            this.copy(dateTime = newDateTimeMillis, date = null)
        } else {
            this.copy(date = newDate.toString(), dateTime = null)
        }
    }
}