package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
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
        // 🔥 CORRECCIÓN 1: Pasamos el TIPO correcto, no la ACCIÓN
        scheduleAlarm(8, 0, ID_MORNING, AlarmReceiver.TYPE_MORNING_BRIEFING)
    }

    // Programamos la alarma de la noche (20:00)
    fun scheduleEveningDebrief() {
        // 🔥 CORRECCIÓN 1: Pasamos el TIPO correcto
        scheduleAlarm(20, 0, ID_EVENING, AlarmReceiver.TYPE_EVENING_DEBRIEF)
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
            // 🔥 CORRECCIÓN 2: Usamos la MISMA acción que espera el Receiver
            action = AlarmReceiver.ALARM_ACTION
            putExtra("EXTRA_TYPE", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmInfo, pendingIntent)

        Log.d("SYCROM_ALARM", "Briefing ($type) programado para: ${calendar.time}")
    }
}