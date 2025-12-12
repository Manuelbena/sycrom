package com.manuelbena.synkron

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import com.manuelbena.synkron.data.local.notification.QuickAccessService
import com.manuelbena.synkron.data.scheduler.BriefingScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var briefingScheduler: BriefingScheduler

    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_ALERTS_V12"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Iniciamos los briefings diarios (8:00 y 20:00)
        briefingScheduler.scheduleMorningBriefing()
        briefingScheduler.scheduleEveningDebrief()

        val serviceIntent = Intent(this, QuickAccessService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // --- Canal de Alarmas (Pantalla Completa / Sonido Fuerte) ---
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

            // --- Canal de Notificaciones (Recordatorios Estándar) ---
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
            Log.d("SYCROM_DEBUG", "APP: Canales de notificación V12 creados")
        }
    }
}