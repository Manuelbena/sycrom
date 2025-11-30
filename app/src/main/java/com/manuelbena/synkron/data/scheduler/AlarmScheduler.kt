package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.models.ReminderMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskDomain) {
        // Filtramos recordatorios que sean ALARMA o NOTIFICACIÓN
        val remindersToSchedule = task.reminders.overrides.filter {
            it.method.equals(ReminderMethod.ALARM.name, ignoreCase = true) ||
            it.method.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true)
        }

        android.util.Log.d("SYCROM_ALARM", "Programando ${remindersToSchedule.size} avisos para: ${task.summary}")

        val startMillis = task.start?.dateTime?.toInstant()?.toEpochMilli() ?: return

        remindersToSchedule.forEach { reminder ->
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)

            if (triggerTime > System.currentTimeMillis()) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = "com.manuelbena.synkron.ACTION_ALARM_TRIGGER" // La acción única que creamos antes
                    putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                    putExtra("EXTRA_TASK_ID", task.id.toString())
                    putExtra("EXTRA_TYPE", reminder.method)}

                val requestCode = task.id.hashCode() + reminder.minutes

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                android.util.Log.d(
                    "SYCROM_ALARM",
                    "Programado ${reminder.method} para ${task.summary}"
                )
            }
        }
    }

    fun cancel(task: TaskDomain) {
        // Implementar lógica de cancelación usando los mismos RequestCodes
    }
}