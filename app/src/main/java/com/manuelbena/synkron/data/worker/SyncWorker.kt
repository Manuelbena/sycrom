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
 * ESTRATEGIA: Read & Create.
 * - SUBIR: Solo tareas nuevas (que no tengan ID de Google).
 * - BAJAR: Descarga eventos de Google de los próximos 30 días.
 * - BORRAR/EDITAR: No se sincronizan hacia Google (Solo Local).
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
            Log.d("SyncWorker", "🔄 WORKER: Iniciando sincronización (Read & Create)...")

            // ------------------------------------------------------------
            // 1. SUBIR NUEVAS TAREAS (Create) 📤
            // ------------------------------------------------------------
            // Solo subimos las tareas locales que aún no tienen enlace con Google.
            val pendingTasks = taskDao.getTasksWithoutGoogleId()

            if (pendingTasks.isNotEmpty()) {
                Log.d("SyncWorker", "📤 Subiendo ${pendingTasks.size} tareas nuevas a Google.")
            }

            pendingTasks.forEach { entity ->
                // Ignoramos fantasmas (copias locales de series) y tareas que ya tengan ID.
                if (entity.googleCalendarId == "LOCAL_GHOST" || !entity.googleCalendarId.isNullOrEmpty()) {
                    return@forEach
                }

                val domainTask = entity.toDomain()

                // Usamos INSERT para crear en Google.
                val googleId = googleRepo.insertEvent(domainTask)

                if (googleId != null) {
                    // Si se creó con éxito, guardamos el ID para evitar duplicados futuros.
                    val updatedEntity = entity.copy(googleCalendarId = googleId)
                    taskDao.updateTask(updatedEntity)
                    Log.d("SyncWorker", "✅ Creada en Google: ${domainTask.summary} -> ID: $googleId")
                } else {
                    Log.e("SyncWorker", "❌ Falló la creación de: ${domainTask.summary}")
                }
            }

            // ------------------------------------------------------------
            // 2. BAJAR ACTUALIZACIONES (Read) 📥
            // ------------------------------------------------------------
            // Sincronizamos los próximos 30 días para mantener la agenda al día.
            syncUpcomingMonth()

            Log.d("SyncWorker", "🏁 Sincronización finalizada.")
            Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "❌ Error en sincronización: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    /**
     * Descarga eventos de Google (próximos 30 días) y los fusiona con la base de datos local.
     */
    private suspend fun syncUpcomingMonth() {
        val now = System.currentTimeMillis()
        val rangeEnd = now + (30L * 24 * 60 * 60 * 1000) // +30 días

        val googleTasks = googleRepo.fetchEventsBetween(now, rangeEnd)

        if (googleTasks.isNotEmpty()) {
            Log.d("SyncWorker", "📥 Descargados ${googleTasks.size} eventos recientes.")

            googleTasks.forEach { googleTask ->
                val gId = googleTask.googleCalendarId ?: return@forEach

                // 1. Buscamos si ya existe por su ID
                var localEntity = taskDao.getTaskByGoogleId(gId)

                // 2. Si no existe ID, buscamos por coincidencia (Nombre + Fecha)
                // Esto evita duplicados si el ID cambió en el servidor.
                if (localEntity == null) {
                    val zoneId = ZoneId.systemDefault()
                    val taskDate = calculateLocalDate(googleTask)
                    val startOfDay = taskDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val endOfDay = taskDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli()

                    localEntity = taskDao.findLocalCandidate(googleTask.summary, startOfDay, endOfDay)
                }

                if (localEntity != null) {
                    // UPDATE: Actualizamos nuestra copia local con lo que dice Google.
                    val updatedEntity = localEntity.copy(
                        googleCalendarId = gId,
                        summary = googleTask.summary,
                        description = googleTask.description ?: localEntity.description,

                        isDone = localEntity.isDone,

                        typeTask = googleTask.typeTask,
                        date = calculateGoogleDateMillis(googleTask),
                        hour = if (googleTask.start?.dateTime != null) {
                            LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(googleTask.start.dateTime), ZoneId.systemDefault()).hour
                        } else localEntity.hour
                    )
                    taskDao.updateTask(updatedEntity)
                } else {
                    // INSERT: Es un evento nuevo de Google que no teníamos.
                    val newEntity = googleTask.toEntity().copy(
                        id = 0, // ID 0 para autogenerar
                        googleCalendarId = gId,
                        typeTask = googleTask.typeTask.ifEmpty { "Personal" },
                        priority = "Media",
                        date = calculateGoogleDateMillis(googleTask)
                    )
                    taskDao.insertTask(newEntity)
                }
            }
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