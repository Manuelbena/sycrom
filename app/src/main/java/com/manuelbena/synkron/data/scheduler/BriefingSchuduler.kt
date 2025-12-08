package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.manuelbena.synkron.presentation.models.ReminderMethod
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject

class BriefingScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    companion object {
        const val ID_MORNING = 1001
        const val ID_EVENING = 1002
    }

    // Programamos la alarma de la mañana (8:00)
    fun scheduleMorningBriefing() {
        scheduleAlarm(8, 0, ID_MORNING, AlarmReceiver.ALARM_ACTION)
    }

    // Programamos la alarma de la noche (20:00)
    fun scheduleEveningDebrief() {
        scheduleAlarm(20, 0, ID_EVENING, AlarmReceiver.ALARM_ACTION)
    }

    private fun scheduleAlarm(hour: Int, minute: Int, reqCode: Int, type: String) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, programamos para mañana
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.manuelbena.synkron.ACTION_BRIEFING" // Acción específica
            putExtra("EXTRA_TYPE", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 🔥 LA CLAVE: Usamos setAlarmClock igual que en las tareas
        // Esto garantiza que el sistema nos despierte sí o sí.
        val alarmInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmInfo, pendingIntent)
    }
}