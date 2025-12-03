package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskDomain) {
        val allReminders = task.reminders.overrides

        Log.d("SYCROM_DEBUG", "SCHEDULER: Procesando ID=${task.id}. Total: ${allReminders.size}")

        val remindersToSchedule = allReminders.filter { reminder ->
            val method = reminder.method.trim().lowercase()
            method.contains("alarm") || method.contains("notif") || method.contains("popup")
        }

        // --- CORRECCIÓN AQUÍ ---
        // Antes: task.start?.dateTime?.toInstant()?.toEpochMilli()
        // Ahora: Usamos el Long directamente. Si es nulo, salimos.
        val startMillis = task.start?.dateTime ?: return
        // -----------------------

        // --- PREPARAR DATOS RICOS PARA SYCROM ---
        val subtasksSummary = if (task.subTasks.isNotEmpty()) {
            task.subTasks.joinToString("\n") { "• ${it.title}" }
        } else {
            ""
        }

        remindersToSchedule.forEach { reminder ->
            // Ahora la resta funciona perfecto porque startMillis es Long
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)
            val now = System.currentTimeMillis()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ALARM_ACTION // Asegúrate de que esta constante sea pública en AlarmReceiver
                putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                putExtra("EXTRA_TITLE", task.summary)
                putExtra("EXTRA_DESC", task.description ?: "")
                putExtra("EXTRA_SUBTASKS", subtasksSummary)
                putExtra("EXTRA_LOCATION", task.location ?: "")
                putExtra("EXTRA_TASK_ID", task.id.toString())

                val method = reminder.method.lowercase()
                val typeToSend = if (method.contains("alarm")) "ALARM" else "NOTIFICATION"
                putExtra("EXTRA_TYPE", typeToSend)
                putExtra("EXTRA_TIME", triggerTime)
            }

            val requestCode = task.id.hashCode() + reminder.minutes

            if (triggerTime > now) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Usamos setAlarmClock para máxima precisión
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                Log.d("SYCROM_DEBUG", "SCHEDULER: PROGRAMADA (Futuro) -> ${Date(triggerTime)}")

            } else if (triggerTime > (now - 15 * 60 * 1000)) {
                // Si pasó hace menos de 15 mins, la lanzamos ya
                Log.d("SYCROM_DEBUG", "SCHEDULER: Disparando INMEDIATAMENTE")
                context.sendBroadcast(intent)
            } else {
                Log.w("SYCROM_DEBUG", "SCHEDULER: Omitido por antigüedad")
            }
        }
    }

    fun cancel(task: TaskDomain) {
        try {
            task.reminders.overrides.forEach { reminder ->
                val requestCode = task.id.hashCode() + reminder.minutes
                val intent = Intent(context, AlarmReceiver::class.java)
                intent.action = AlarmReceiver.ALARM_ACTION
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel() // Buena práctica: cancelar también el PendingIntent
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}