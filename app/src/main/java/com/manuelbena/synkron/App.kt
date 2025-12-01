package com.manuelbena.synkron

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        const val NOTIFICATION_CHANNEL_ID = "TASK_NOTIFICATION_CHANNEL" // <--- NUEVO ID
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 1. CANAL DE ALARMAS (Sonido fuerte, Prioridad Máxima)
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
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
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(alarmSound, audioAttributes)
            }

            // 2. CANAL DE NOTIFICACIONES (Sonido estándar, Prioridad Default) <--- ESTO FALTABA
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH // <--- CAMBIAR A HIGH
            ).apply {
                description = "Notificaciones estándar para tareas"
                enableVibration(true)
                // Opcional: Patrón de vibración corto para diferenciar de la alarma
                vibrationPattern = longArrayOf(0, 500)
                // Usa el sonido de notificación predeterminado del sistema
                val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notificationAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(notificationSound, notificationAttributes)
            }

            // Creamos ambos canales
            manager.createNotificationChannels(listOf(alarmChannel, notificationChannel))
        }
    }
}