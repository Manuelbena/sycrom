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
        Log.d("SYCROM_DEBUG", "SCHEDULER: Programando ID=${task.id}. Recordatorios: ${allReminders.size}")

        // Filtramos solo métodos válidos
        val remindersToSchedule = allReminders.filter { reminder ->
            val method = reminder.method.trim().lowercase()
            method.contains("alarm") || method.contains("notif") || method.contains("popup")
        }

        val startMillis = task.start?.dateTime ?: return

        val subtasksSummary = if (task.subTasks.isNotEmpty()) {
            task.subTasks.joinToString("\n") { "• ${it.title}" }
        } else { "" }

        remindersToSchedule.forEach { reminder ->
            // Calculamos momento de disparo
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)
            val now = System.currentTimeMillis()

            // 1. Definir el TIPO (Alarma o Notificación) para que el Receiver sepa qué hacer
            val method = reminder.method.lowercase()
            val typeToSend = if (method.contains("alarm")) "ALARM" else "NOTIFICATION"

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ALARM_ACTION
                putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                putExtra("EXTRA_TITLE", task.summary)
                putExtra("EXTRA_DESC", task.description ?: "")
                putExtra("EXTRA_SUBTASKS", subtasksSummary)
                putExtra("EXTRA_LOCATION", task.location ?: "")
                putExtra("EXTRA_TASK_ID", task.id.toString())
                putExtra("EXTRA_TYPE", typeToSend)
                putExtra("EXTRA_TIME", triggerTime)
            }

            // Usamos un RequestCode único combinando ID tarea + minutos
            val requestCode = task.id.hashCode() + reminder.minutes

            // [CORRECCIÓN CRÍTICA] Solo programamos si es FUTURO.
            // Eliminamos el 'else' que disparaba sendBroadcast inmediato.
            // Esto soluciona que al editar te salten notificaciones viejas.
            if (triggerTime > now) {
                Log.d("SYCROM_DEBUG", "SCHEDULER: Programando $typeToSend para ${Date(triggerTime)} (ID: ${task.id})")
                scheduleFutureAlarm(triggerTime, pendingIntent(requestCode, intent))
            } else {
                Log.w("SYCROM_DEBUG", "SCHEDULER: Omitiendo recordatorio pasado para ID ${task.id}")
            }
        }
    }

    private fun scheduleFutureAlarm(triggerTime: Long, pendingIntent: PendingIntent) {
        try {
            // [QUITAR RESTRICCIONES] Gestión inteligente de permisos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("SYCROM_DEBUG", "SCHEDULER: Sin permiso Exacto. Usando set() estándar (Inexacta).")
                    // Fallback a alarma estándar (puede variar unos minutos pero FUNCIONA sin crashear)
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    return
                }
            }

            // INTENTO PREFERIDO: Alarma exacta tipo reloj (máxima prioridad)
            val alarmInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
            alarmManager.setAlarmClock(alarmInfo, pendingIntent)

        } catch (e: SecurityException) {
            // Plan C: Si todo falla por seguridad, intentamos lo básico
            Log.e("SYCROM_DEBUG", "SCHEDULER: Error de seguridad. Usando set() básico.")
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (e2: Exception) { e2.printStackTrace() }
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
                // Usamos FLAG_NO_CREATE para ver si existe antes de intentar cancelar
                val pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pi != null) {
                    alarmManager.cancel(pi)
                    pi.cancel()
                    Log.d("SYCROM_DEBUG", "SCHEDULER: Alarma cancelada reqCode=$requestCode")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}