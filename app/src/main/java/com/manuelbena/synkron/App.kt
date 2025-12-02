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
        // NUEVO ID V12 para resetear configuración en VIVO
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_ALERTS_V12"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Canal Alarmas
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

            // Canal Notificaciones
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_HIGH // OBLIGATORIO
            ).apply {
                description = "Notificaciones importantes"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)

                // Usamos sonido de ALARMA también para notificaciones para asegurar que suene
                // Esto a veces ayuda en móviles chinos que silencian notificaciones normales
                val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val notificationAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(notificationSound, notificationAttributes)

                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true) // Mostrar puntito en el icono de la app
            }

            manager.createNotificationChannels(listOf(alarmChannel, notificationChannel))
            Log.d("SYCROM_DEBUG", "APP: Canal V12 creado")
        }
    }
}