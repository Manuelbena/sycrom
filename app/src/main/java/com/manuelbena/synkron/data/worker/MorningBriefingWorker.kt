package com.manuelbena.synkron.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.manuelbena.synkron.data.local.notification.NotificationHelper
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

@HiltWorker
class MorningBriefingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ITaskRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        // Obtenemos una instantánea de las tareas de hoy
        val tasks = repository.getTasksForDate(today).first()

        if (tasks.isNotEmpty()) {
            val taskCount = tasks.size
            // Buscamos la hora de fin de la última tarea
            val lastTaskEnd = tasks.mapNotNull { it.end }
            val endTimeString = lastTaskEnd

            val title = "☀️ Buenos días, Manuel. Sistemas listos."
            val body = "Detecto $taskCount misiones para hoy 🚀. Tu hora estimada de finalización es a las $endTimeString 🏁. ¿Vamos a por ello?"

            notificationHelper.showStandardNotification(title, body, null)
        } else {
            // Mensaje si no hay nada (Opcional, para que sepas que está vivo)
            notificationHelper.showStandardNotification(
                "☀️ Agenda despejada",
                "Hoy no tienes misiones registradas. ¿Día libre o planificamos algo? 🤔",
                null
            )
        }

        return Result.success()
    }
}