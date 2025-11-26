package com.manuelbena.synkron

import android.app.Application
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.ContextCompat.getSystemService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // 1. Definimos que el sonido es de tipo ALARMA (no notificación)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM) // <--- CLAVE: Usa volumen de alarma
            .build()

        // 2. Cambiamos el ID para asegurar que se crea nuevo
        val channel = android.app.NotificationChannel(
            "ALARM_CHANNEL_V2", // Nuevo ID
            "Alarmas Sycrom",
            android.app.NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal prioritario para alarmas"
            enableVibration(true)
            // Patrón de vibración tipo alarma: largo, pausa, largo...
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            setSound(alarmSound, audioAttributes)
        }

        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}