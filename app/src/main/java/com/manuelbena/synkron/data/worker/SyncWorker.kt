package com.manuelbena.synkron.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.data.mappers.toDomain
import com.manuelbena.synkron.data.mappers.toEntity
import com.manuelbena.synkron.data.repository.GoogleCalendarRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Worker encargado de la sincronización en segundo plano.
 * ESTRATEGIA: Híbrida.
 * - BORRAR: Prioritario. Si recibe ID, borra y termina.
 * - SUBIR: Solo tareas nuevas.
 * - BAJAR: Descarga eventos recientes.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val googleRepo: GoogleCalendarRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // ------------------------------------------------------------
            // 1. GESTIONAR BORRADOS🗑️
            // ------------------------------------------------------------
            val deleteId = inputData.getString("DELETE_GOOGLE_ID")

            if (!deleteId.isNullOrEmpty() && deleteId != "LOCAL_GHOST") {
                Log.d("SyncWorker", "🗑️ WORKER: Recibida orden de borrar: $deleteId")

                val success = googleRepo.deleteEvent(deleteId)

                if (success) {
                    Log.d("SyncWorker", "✅ Eliminado correctamente de Google Calendar")
                } else {
                    Log.w("SyncWorker", "⚠️ No se pudo borrar en Google (¿Ya no existe?)")
                }
                return@withContext Result.success()
            }

            // ------------------------------------------------------------
            // SI NO ES BORRADO -> EJECUTAMOS SYNC NORMAL
            // ------------------------------------------------------------
            Log.d("SyncWorker", "🔄 WORKER: Iniciando sincronización (Read & Create)...")

            // ------------------------------------------------------------
            // 2. SUBIR NUEVAS TAREAS (Create) 📤
            // ------------------------------------------------------------
            val pendingTasks = taskDao.getTasksWithoutGoogleId()

            if (pendingTasks.isNotEmpty()) {
                Log.d("SyncWorker", "📤 Subiendo ${pendingTasks.size} tareas nuevas a Google.")
            }

            pendingTasks.forEach { entity ->
                if (entity.googleCalendarId == "LOCAL_GHOST" || !entity.googleCalendarId.isNullOrEmpty()) {
                    return@forEach
                }

                val domainTask = entity.toDomain()
                val googleId = googleRepo.insertEvent(domainTask)

                if (googleId != null) {
                    val updatedEntity = entity.copy(googleCalendarId = googleId)
                    taskDao.updateTask(updatedEntity)
                    Log.d("SyncWorker", "✅ Creada en Google: ${domainTask.summary} -> ID: $googleId")
                } else {
                    Log.e("SyncWorker", "❌ Falló la creación de: ${domainTask.summary}")
                }
            }

            // ------------------------------------------------------------
            // 3. BAJAR ACTUALIZACIONES (Read) 📥
            // ------------------------------------------------------------
            syncUpcomingMonth()
            
            // 4. LIMPIEZA DE HUÉRFANOS 🧹
            cleanOrphanedTasks()

            Log.d("SyncWorker", "🏁 Sincronización finalizada.")
            Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Error en sincronización: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Descarga eventos de Google y los fusiona con la base de datos local.
     */
    private suspend fun syncUpcomingMonth() {
        val now = System.currentTimeMillis()
        
        // Ventana amplia: desde hace 7 días hasta dentro de 90 días
        val rangeStart = now - (7L * 24 * 60 * 60 * 1000) 
        val rangeEnd = now + (90L * 24 * 60 * 60 * 1000)

        val googleTasks = googleRepo.fetchEventsBetween(rangeStart, rangeEnd)

        if (googleTasks.isNotEmpty()) {
            Log.d("SyncWorker", "📥 Descargados ${googleTasks.size} eventos de Google.")

            googleTasks.forEach { googleTask ->
                val gId = googleTask.googleCalendarId ?: return@forEach

                var localEntity = taskDao.getTaskByGoogleId(gId)

                if (localEntity == null) {
                    val zoneId = ZoneId.systemDefault()
                    val taskDate = calculateLocalDate(googleTask)
                    val startOfDay = taskDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val endOfDay = taskDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

                    localEntity = taskDao.findLocalCandidate(googleTask.summary, startOfDay, endOfDay)
                }

                if (localEntity != null) {
                    val updatedEntity = localEntity.copy(
                        googleCalendarId = gId,
                        summary = googleTask.summary,
                        description = googleTask.description ?: localEntity.description,
                        isDone = localEntity.isDone,
                        typeTask = googleTask.typeTask,
                        date = calculateGoogleDateMillis(googleTask),
                        hour = calculateGoogleHourMinutes(googleTask)
                    )
                    taskDao.updateTask(updatedEntity)
                } else {
                    val newEntity = googleTask.toEntity().copy(
                        id = 0,
                        googleCalendarId = gId,
                        typeTask = googleTask.typeTask.ifEmpty { "Personal" },
                        priority = "Media",
                        date = calculateGoogleDateMillis(googleTask),
                        hour = calculateGoogleHourMinutes(googleTask)
                    )
                    taskDao.insertTask(newEntity)
                    Log.d("SyncWorker", "🆕 Importado desde Google: ${googleTask.summary}")
                }
            }
        }
    }

    private fun calculateGoogleHourMinutes(googleTask: TaskDomain): Int {
        val dt = googleTask.start?.dateTime
        return if (dt != null) {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = dt
            (cal.get(java.util.Calendar.HOUR_OF_DAY) * 60) + cal.get(java.util.Calendar.MINUTE)
        } else {
            -1 // Todo el día
        }
    }

    /**
     * Borra las tareas locales que tienen un ID de Google pero ya no existen en el calendario remoto.
     */
    private suspend fun cleanOrphanedTasks() {
        val now = System.currentTimeMillis()
        val start = now - (7L * 24 * 60 * 60 * 1000)
        val end = now + (90L * 24 * 60 * 60 * 1000)

        val googleTasks = googleRepo.fetchEventsBetween(start, end)
        val googleIds = googleTasks.mapNotNull { it.googleCalendarId }

        if (googleIds.isNotEmpty()) {
            taskDao.deleteOrphanedTasks(start, end, googleIds)
            Log.d("SyncWorker", "🧹 Limpieza de huérfanos completada.")
        }
    }

    // --- Helpers de Fecha ---

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

    private fun calculateLocalDate(googleTask: TaskDomain): LocalDate {
        val millis = calculateGoogleDateMillis(googleTask)
        return LocalDate.ofInstant(java.time.Instant.ofEpochMilli(millis), ZoneId.systemDefault())
    }
}
