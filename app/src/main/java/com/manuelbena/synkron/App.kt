package com.manuelbena.synkron

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        // ID Definitivo. NO LO CAMBIES más o perderás el hilo.
        const val NOTIFICATION_CHANNEL_ID = "TASK_NOTIFICATION_FINAL_V2"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

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
                "Recordatorios Sycrom",
                NotificationManager.IMPORTANCE_HIGH // IMPORTANTE: HIGH
            ).apply {
                description = "Notificaciones flotantes de tareas"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            manager.createNotificationChannels(listOf(alarmChannel, notificationChannel))
            Log.d("SYCROM_DEBUG", "APP: Canales inicializados OK.")
        }
    }
}