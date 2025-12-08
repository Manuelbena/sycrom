package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
        Log.d("SYCROM_DEBUG", "SCHEDULER: Procesando ID=${task.id}. Total reminders: ${allReminders.size}")

        val remindersToSchedule = allReminders.filter { reminder ->
            val method = reminder.method.trim().lowercase()
            method.contains("alarm") || method.contains("notif") || method.contains("popup")
        }

        val startMillis = task.start?.dateTime ?: return

        val subtasksSummary = if (task.subTasks.isNotEmpty()) {
            task.subTasks.joinToString("\n") { "• ${it.title}" }
        } else { "" }

        remindersToSchedule.forEach { reminder ->
            // Si reminder.minutes es 0, triggerTime == startMillis (MOMENTO EXACTO)
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)
            val now = System.currentTimeMillis()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ALARM_ACTION
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

            // Lógica "SIN RESTRICCIONES":
            if (triggerTime > now) {
                // FUTURO: Intentamos poner alarma exacta
                scheduleFutureAlarm(triggerTime, pendingIntent(requestCode, intent))
            } else {
                // PASADO (Aunque sea por 1 segundo): ¡DISPARAR YA!
                // Eliminamos la restricción de "solo si hace menos de 15 min".
                // Si la tarea era para hace 1 hora y la acabas de crear/editar, te avisará ahora.
                Log.w("SYCROM_DEBUG", "SCHEDULER: Hora pasada. Disparando INMEDIATAMENTE.")
                context.sendBroadcast(intent)
            }
        }
    }

    private fun scheduleFutureAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            // Verificamos permiso en Android 12+ para evitar crashes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.e("SYCROM_DEBUG", "SCHEDULER: No tengo permiso de Alarmas Exactas. Usando setExact (menos preciso).")
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    return
                }
            }

            // INTENTO PREFERIDO: Alarma exacta tipo reloj despertador
            val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
            alarmManager.setAlarmClock(alarmInfo, pendingIntent)
            Log.d("SYCROM_DEBUG", "SCHEDULER: ✅ Alarma exacta programada para: ${Date(triggerTime)}")

        } catch (e: SecurityException) {
            // Plan B: Si explota por permisos, intentamos setExactAndAllowWhileIdle
            Log.e("SYCROM_DEBUG", "SCHEDULER: Error de seguridad. Intentando Plan B.")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pendingIntent(reqCode: Int, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun cancel(task: TaskDomain) {
        try {
            task.reminders.overrides.forEach { reminder ->
                val requestCode = task.id.hashCode() + reminder.minutes
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ALARM_ACTION
                }
                val pi = pendingIntent(requestCode, intent)
                alarmManager.cancel(pi)
                pi.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}