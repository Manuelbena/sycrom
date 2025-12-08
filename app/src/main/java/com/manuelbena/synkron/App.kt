package com.manuelbena.synkron

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.manuelbena.synkron.data.scheduler.AlarmReceiver
import com.manuelbena.synkron.data.scheduler.BriefingScheduler
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import javax.inject.Inject

@HiltAndroidApp
class App : Application() { // Ya no necesitas Configuration.Provider ni HiltWorkerFactory aquí

    @Inject
    lateinit var briefingScheduler: BriefingScheduler // 🔥 Inyectamos
    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_ALERTS_V12"

        private const val ID_MORNING_ALARM = 1001
        private const val ID_EVENING_ALARM = 1002
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        briefingScheduler.scheduleMorningBriefing()
        briefingScheduler.scheduleEveningDebrief()
    }

    /**
     * Programa las alarmas repetitivas de las 8:00 y 20:00
     */
    private fun scheduleDailyAlarms() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. MAÑANA (08:00)
        scheduleRepeatingAlarm(alarmManager, 8, 0, ID_MORNING_ALARM, AlarmReceiver.ALARM_ACTION)

        // 2. NOCHE (20:00)
        scheduleRepeatingAlarm(alarmManager, 20, 0, ID_EVENING_ALARM, AlarmReceiver.ALARM_ACTION)
    }

    private fun scheduleRepeatingAlarm(alarmManager: AlarmManager, hour: Int, minute: Int, reqCode: Int, type: String) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("EXTRA_TYPE", type)
        }

        // FLAG_UPDATE_CURRENT asegura que si cambiamos la lógica, se actualice el intent
        val pendingIntent = PendingIntent.getBroadcast(
            this, reqCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // Si la hora ya pasó hoy, programar para mañana
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Usamos setInexactRepeating para ser amigables con la batería,
        // pero asegurando que se repita cada día (INTERVAL_DAY)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d("SYCROM_ALARM", "Alarma $type programada para: ${calendar.time}")
    }

    private fun createNotificationChannels() {
        // ... (Tu código existente de canales se queda IGUAL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Copia aquí el bloque de canales que ya tenías en tu App.kt anterior
            // (No hace falta cambiar nada en los canales)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Alarmas Prioritarias",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para alarmas de pantalla completa"
                enableVibration(true)
                setSound(alarmSound, alarmAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones importantes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notificationAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(notificationSound, notificationAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            manager.createNotificationChannels(listOf(alarmChannel, notificationChannel))
        }
    }
}