package com.manuelbena.synkron.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manuelbena.synkron.data.local.notification.NotificationHelper
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class EveningDebriefWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ITaskRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val tasks = repository.getTasksForDate(today).first()

        if (tasks.isNotEmpty()) {
            val total = tasks.size
            val done = tasks.count { it.isDone }
            val percentage = (done * 100) / total

            val (emoji, phrase) = when {
                percentage == 100 -> "🏆" to "¡Misión Cumplida! Rendimiento máximo."
                percentage >= 80 -> "🔥" to "Gran ritmo hoy. Casi perfecto."
                percentage >= 50 -> "⚡" to "Buen avance. Mañana rematamos el resto."
                else -> "⚠️" to "Día complicado. Reprogramemos para mañana."
            }

            notificationHelper.showStandardNotification(
                title = "$emoji Resumen Diario: $percentage% Completado",
                message = "$phrase Tienes ${total - done} tareas pendientes. ¿Cerramos sistema? 💤",
                taskId = null
            )
        }

        return Result.success()
    }
}