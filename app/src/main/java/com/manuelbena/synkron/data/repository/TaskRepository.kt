package com.manuelbena.synkron.data.repository

import android.util.Log
import androidx.work.*
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.data.mappers.toTaskDomain
import com.manuelbena.synkron.data.remote.n8n.N8nApi
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatRequest
import com.manuelbena.synkron.data.scheduler.AlarmScheduler
import com.manuelbena.synkron.data.workers.SyncWorker
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.GoogleEventDateTime
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.util.toLocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val alarmScheduler: AlarmScheduler,
    private val api: N8nApi,
    private val workManager: WorkManager,
    private val googleCalendarRepository: GoogleCalendarRepository
) : ITaskRepository {

    // Rate Limiter: Evita spam de llamadas a la API (máx 1 cada minuto en auto-sync)
    private var lastAutoSyncTime: Long = System.currentTimeMillis() - 600_000L
    private val MIN_AUTO_SYNC_INTERVAL = 60_000L

    // ----------------------------------------------------------------
    // 1. LECTURA (READ)
    // ----------------------------------------------------------------

    override fun getAllTasks(): Flow<List<TaskDomain>> =
        taskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }

    override fun getTasksBetweenDates(start: Long, end: Long): Flow<List<TaskDomain>> =
        taskDao.getTasksBetweenDates(start, end).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getTaskById(id: Int): TaskDomain? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun hasAllDayTaskOnDate(date: LocalDate, excludedId: Int): Boolean {
        val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return taskDao.checkAllDayTaskExists(millis, excludedId) > 0
    }

    override fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return taskDao.getTasksBetween(startOfDay, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun refreshTasksForDate(date: LocalDate) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (now - lastAutoSyncTime < MIN_AUTO_SYNC_INTERVAL) {
                Log.v("SycromSync", "⏳ Sync ignorada (Enfriamiento)")
                return@withContext
            }
            lastAutoSyncTime = now

            try {
                Log.d("SycromSync", "🚀 Refrescando fecha: $date")
                val zoneId = ZoneId.systemDefault()
                val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
                val safeStart = startOfDay - (48 * 60 * 60 * 1000)
                val safeEnd = startOfDay + (48 * 60 * 60 * 1000)

                val googleTasks = googleCalendarRepository.fetchEventsBetween(safeStart, safeEnd)
                processGoogleList(googleTasks)
            } catch (e: Exception) {
                Log.e("SycromSync", "Error refreshTasksForDate: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------------------
    // 2. ESCRITURA (WRITE) - ESTRATEGIA: CREATE & LOCAL ONLY
    // ----------------------------------------------------------------

    override suspend fun insertTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            val taskToSave = task.copy(googleCalendarId = null)

            if (task.synkronRecurrenceDays.isNotEmpty()) {
                insertRecursiveSeries(taskToSave)
            } else {
                insertSingleTask(taskToSave, null)
            }
            scheduleSyncWorker()
        }
    }

    override suspend fun updateTask(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            // 1. Recuperar ID para no perder el enlace
            val currentEntity = taskDao.getTaskById(task.id)
            if (currentEntity != null) alarmScheduler.cancel(currentEntity.toDomain())

            val taskToSave = if (task.googleCalendarId.isNullOrEmpty() && currentEntity?.googleCalendarId != null) {
                task.copy(googleCalendarId = currentEntity.googleCalendarId)
            } else {
                task
            }

            // 2. Guardar en local
            taskDao.updateTask(taskToSave.toEntity())
            alarmScheduler.schedule(taskToSave)

            Log.d("SycromSync", "✏️ Tarea actualizada en LOCAL. Google no se notifica.")
        }
    }

    override suspend fun deleteTaskInstance(task: TaskDomain) {
        withContext(Dispatchers.IO) {
            alarmScheduler.cancel(task)
            taskDao.deleteTask(task.toEntity())
            Log.d("SycromSync", "🗑️ Tarea borrada en LOCAL.")
        }
    }

    override suspend fun deleteTaskSeries(task: TaskDomain) = deleteTaskInstance(task)
    override suspend fun deleteTask(task: TaskDomain) = deleteTaskInstance(task)

    // ----------------------------------------------------------------
    // 3. SINCRONIZACIÓN MASIVA (MODO ESPEJO 🪞)
    // ----------------------------------------------------------------

    override suspend fun syncYear(year: Int) {
        withContext(Dispatchers.IO) {
            try {
                Log.d("SycromSync", "📅 Sincronizando AÑO (Modo Espejo): $year")
                val zoneId = ZoneId.systemDefault()
                val startOfYear = LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endOfYear = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

                val googleTasks = googleCalendarRepository.fetchEventsBetween(startOfYear, endOfYear)

                if (googleTasks.isNotEmpty()) {
                    // 1. Hay eventos en Google: Actualizamos los que existen
                    processGoogleList(googleTasks)

                    // 2. BORRADO DE HUÉRFANOS:
                    // Si tengo una tarea local que NO está en esta lista de Google, la borro.
                    val validIds = googleTasks.mapNotNull { it.googleCalendarId }
                    if (validIds.isNotEmpty()) {
                        Log.d("SycromSync", "🧹 Limpiando tareas que ya no existen en Google...")
                        taskDao.deleteOrphanedTasks(startOfYear, endOfYear, validIds)
                    }
                } else {
                    // 3. CASO CRÍTICO: Google está vacío para este año.
                    // Si Google no tiene nada, borramos TODO lo local que esté sincronizado.
                    Log.d("SycromSync", "🧽 Año vacío en Google. Borrando todo lo local...")
                    taskDao.deleteAllSyncedTasksInRange(startOfYear, endOfYear)
                }
                Unit
            } catch (e: Exception) {
                Log.e("SycromSync", "❌ Error syncYear: ${e.message}")
            }
        }
    }

    override suspend fun syncCurrentMonth() {
        syncSpecificMonth(LocalDate.now())
    }

    override suspend fun syncSpecificMonth(date: LocalDate) {
        withContext(Dispatchers.IO) {
            try {
                val zoneId = ZoneId.systemDefault()
                val startOfMonth = date.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val endOfMonth = date.withDayOfMonth(date.lengthOfMonth()).atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

                val googleTasks = googleCalendarRepository.fetchEventsBetween(startOfMonth, endOfMonth)

                if (googleTasks.isNotEmpty()) {
                    processGoogleList(googleTasks)

                    val validIds = googleTasks.mapNotNull { it.googleCalendarId }
                    if (validIds.isNotEmpty()) {
                        taskDao.deleteOrphanedTasks(startOfMonth, endOfMonth, validIds)
                    }
                } else {
                    // Si el mes está vacío en Google, limpiamos el mes en local
                    Log.d("SycromSync", "🧽 Mes vacío en Google. Borrando local...")
                    taskDao.deleteAllSyncedTasksInRange(startOfMonth, endOfMonth)
                }
                Unit
            } catch (e: Exception) {
                Log.e("SycromSync", "❌ Error syncMonth: ${e.message}")
            }
        }
    }

    // ----------------------------------------------------------------
    // 4. FUSIÓN DE DATOS (MERGE)
    // ----------------------------------------------------------------

    // En TaskRepository.kt

    private suspend fun processGoogleList(googleTasks: List<TaskDomain>) {
        googleTasks.forEach { googleTask ->
            val gId = googleTask.googleCalendarId ?: return@forEach
            val googleDateMillis = calculateGoogleDateMillis(googleTask)

            var localEntity = taskDao.getTaskByGoogleId(gId)

            // ... (Lógica de búsqueda de candidatos sigue igual) ...

            if (localEntity != null) {
                // === UPDATE (Es la misma tarea -> Actualizamos) ===
                val updatedEntity = localEntity.copy(
                    googleCalendarId = gId,
                    summary = googleTask.summary,
                    description = googleTask.description ?: localEntity.description,
                    date = googleDateMillis,
                    isDone = localEntity.isDone,

                    typeTask = googleTask.typeTask,
                    hour = if (googleTask.start?.dateTime != null) {
                        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(googleTask.start.dateTime), ZoneId.systemDefault()).hour
                    } else localEntity.hour
                )
                taskDao.updateTask(updatedEntity)

            } else {
                // === INSERT (Nueva tarea que viene de Google) ===
                // Aquí SÍ usamos googleTask.isDone, porque es nueva y no tenemos opinión local.
                val newEntity = googleTask.toEntity().copy(
                    id = 0,
                    googleCalendarId = gId,
                    isDone = googleTask.isDone, // <--- Aquí sí dejamos el de Google
                    typeTask = googleTask.typeTask.ifEmpty { "Personal" },
                    priority = "Media",
                    date = googleDateMillis
                )
                taskDao.insertTask(newEntity)
            }
        }
    }

    // ----------------------------------------------------------------
    // 5. HELPERS
    // ----------------------------------------------------------------

    private suspend fun insertRecursiveSeries(originalTask: TaskDomain) {
        val batchId = UUID.randomUUID().toString()
        val startDate = originalTask.start?.toLocalDate() ?: LocalDate.now()
        val endDate = startDate.plusYears(1)
        var currentDate = startDate
        var isFirstTask = true

        while (currentDate.isBefore(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value
            if (originalTask.synkronRecurrenceDays.contains(dayOfWeek)) {
                val newStart = originalTask.start?.copyWithNewDate(currentDate)
                val newEnd = originalTask.end?.copyWithNewDate(currentDate)
                val taskToInsert: TaskDomain
                if (isFirstTask) {
                    taskToInsert = originalTask.copy(id = 0, start = newStart, end = newEnd, googleCalendarId = null)
                    isFirstTask = false
                } else {
                    taskToInsert = originalTask.copy(id = 0, start = newStart, end = newEnd, synkronRecurrenceDays = emptyList(), recurrence = emptyList(), parentId = batchId, googleCalendarId = "LOCAL_GHOST")
                }
                insertSingleTask(taskToInsert, if (isFirstTask) null else batchId)
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    private suspend fun insertSingleTask(task: TaskDomain, parentId: String?) {
        val entity = task.toEntity().copy(parentId = parentId)
        val newId = taskDao.insertTask(entity)
        val taskWithId = task.copy(id = newId.toInt(), parentId = parentId)
        scheduleLocalAlarm(taskWithId)
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork("SynkronSyncWork", ExistingWorkPolicy.APPEND_OR_REPLACE, syncRequest)
    }

    private suspend fun scheduleLocalAlarm(task: TaskDomain) {
        val taskDate = task.start?.toLocalDate() ?: LocalDate.now()
        if (taskDate.isBefore(LocalDate.now().plusDays(7))) {
            alarmScheduler.schedule(task)
        }
    }

    private fun calculateGoogleDateMillis(googleTask: TaskDomain): Long {
        return when {
            googleTask.start?.dateTime != null -> googleTask.start.dateTime
            googleTask.start?.date != null -> {
                try {
                    LocalDate.parse(googleTask.start.date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) { System.currentTimeMillis() }
            }
            else -> System.currentTimeMillis()
        }
    }

    private fun GoogleEventDateTime.copyWithNewDate(newDate: LocalDate): GoogleEventDateTime {
        return if (this.dateTime != null) {
            val originalTime = java.time.Instant.ofEpochMilli(this.dateTime).atZone(ZoneId.systemDefault()).toLocalTime()
            val newDateTimeMillis = LocalDateTime.of(newDate, originalTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            this.copy(dateTime = newDateTimeMillis, date = null)
        } else {
            this.copy(date = newDate.toString(), dateTime = null)
        }
    }

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
}