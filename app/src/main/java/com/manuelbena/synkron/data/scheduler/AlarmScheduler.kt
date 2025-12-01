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
        val remindersToSchedule = task.reminders.overrides.filter {
            it.method.equals(ReminderMethod.ALARM.name, ignoreCase = true) ||
                    it.method.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true)
        }

        Log.d("SYCROM_DEBUG", "SCHEDULER: Intentando programar para ID=${task.id}. Recordatorios encontrados: ${remindersToSchedule.size}")

        val startMillis = task.start?.dateTime?.toInstant()?.toEpochMilli() ?: return

        remindersToSchedule.forEach { reminder ->
            // triggerTime = Hora Inicio - Minutos de aviso
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)
            val now = System.currentTimeMillis()

            Log.d("SYCROM_DEBUG", "SCHEDULER: Check hora -> Trigger: ${Date(triggerTime)} vs Ahora: ${Date(now)}")

            if (triggerTime > now) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ALARM_ACTION // Usamos la constante del Receiver
                    putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                    putExtra("EXTRA_TASK_ID", task.id.toString())
                    putExtra("EXTRA_TYPE", reminder.method)
                }

                // RequestCode único: ID Tarea + Minutos offset
                val requestCode = task.id.hashCode() + reminder.minutes

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                Log.d("SYCROM_DEBUG", "SCHEDULER: ¡ÉXITO! Alarma programada a las ${Date(triggerTime)} (Tipo: ${reminder.method})")
            } else {
                Log.w("SYCROM_DEBUG", "SCHEDULER: OMITIDO. La hora ${Date(triggerTime)} ya ha pasado.")
            }
        }
    }

    fun cancel(task: TaskDomain) {
        val reminders = task.reminders.overrides
        reminders.forEach { reminder ->
            val requestCode = task.id.hashCode() + reminder.minutes
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("SYCROM_DEBUG", "SCHEDULER: Alarma cancelada para ID=${task.id}")
        }
    }
}