package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.models.ReminderMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskDomain) {
        val allReminders = task.reminders.overrides

        Log.d("SYCROM_DEBUG", "SCHEDULER: ID=${task.id}. Recordatorios totales: ${allReminders.size}")

        // --- CORRECCIÓN: ACEPTAR "popup" ---
        val remindersToSchedule = allReminders.filter {
            val method = it.method?.trim()?.lowercase() ?: ""

            // Aceptamos: "alarm", "notification" y "popup"
            method.contains("alarm") ||
                    method.contains("notif") ||
                    method.contains("popup")
        }

        Log.d("SYCROM_DEBUG", "SCHEDULER: Recordatorios VÁLIDOS: ${remindersToSchedule.size}")

        val startMillis = task.start?.dateTime?.toInstant()?.toEpochMilli() ?: return

        remindersToSchedule.forEach { reminder ->
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)
            val now = System.currentTimeMillis()

            // Margen de seguridad de 1 minuto
            if (triggerTime > (now - 60000)) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ALARM_ACTION
                    putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                    putExtra("EXTRA_TASK_ID", task.id.toString())

                    // --- TRADUCCIÓN PARA EL RECEIVER ---
                    // Si es "popup", le decimos al Receiver que es "NOTIFICATION" para que use el Helper
                    val method = reminder.method?.lowercase() ?: ""
                    val typeToSend = if (method.contains("alarm")) "ALARM" else "NOTIFICATION"

                    putExtra("EXTRA_TYPE", typeToSend)
                    Log.d("SYCROM_DEBUG", "SCHEDULER: PROGRAMADA a las ${Date(triggerTime)} (Tipo: $typeToSend)")
                }

                val requestCode = task.id.hashCode() + reminder.minutes

                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)


            } else {
                Log.w("SYCROM_DEBUG", "SCHEDULER: Omitido (Hora pasada) ${Date(triggerTime)}")
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
            }
        } catch (e: Exception) {
            Log.e("SYCROM_DEBUG", "Error cancelando: ${e.message}")
        }
    }
}