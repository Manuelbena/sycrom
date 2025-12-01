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
        // CAMBIO DE ID: Usamos uno completamente nuevo para obligar a Android a resetear la config
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_NOTIFICATIONS_FINAL"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 1. CANAL DE ALARMAS
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

            // 2. CANAL DE NOTIFICACIONES (CORREGIDO PARA VIVO/OPPO)
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Recordatorios Sycrom", // Nombre visible para el usuario
                NotificationManager.IMPORTANCE_HIGH // OBLIGATORIO: HIGH para Heads-up
            ).apply {
                description = "Notificaciones flotantes de tareas"
                enableVibration(true)
                // Patrón de vibración específico para diferenciarlo
                vibrationPattern = longArrayOf(0, 500, 200, 500)

                val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notificationAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(notificationSound, notificationAttributes)

                // CRÍTICO: Forzar visibilidad pública en el propio CANAL
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            // Borrar canales antiguos para evitar confusión (Opcional pero recomendado)
            // manager.deleteNotificationChannel("TASK_NOTIFICATION_CHANNEL_V5")

            manager.createNotificationChannels(listOf(alarmChannel, notificationChannel))
            Log.d("SYCROM_DEBUG", "APP: Canales creados con ID: $NOTIFICATION_CHANNEL_ID")
        }
    }
}