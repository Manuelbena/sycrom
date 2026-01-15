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
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.manuelbena.synkron.data.local.notification.QuickAccessService
import com.manuelbena.synkron.data.scheduler.BriefingScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider { // <--- 1. Implementa la interfaz

    // --- INYECCIONES ---
    @Inject lateinit var workerFactory: HiltWorkerFactory // Para WorkManager
    @Inject lateinit var briefingScheduler: BriefingScheduler // Para tus briefings

    // --- CONFIGURACIÓN WORKMANAGER (Requerido por Hilt) ---
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    // --- CONSTANTES ---
    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_ALERTS_V12"
    }

    // --- LIFECYCLE ---
    override fun onCreate() {
        super.onCreate()

        // 1. Crear canales de notificación
        createNotificationChannels()

        // 2. Programar Briefings diarios (8:00 y 20:00)
        briefingScheduler.scheduleMorningBriefing()
        briefingScheduler.scheduleEveningDebrief()

        // 3. Iniciar servicio de acceso rápido (Foreground)
        val serviceIntent = Intent(this, QuickAccessService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        androidx.work.WorkManager.getInstance(this).cancelAllWork()

        setupPeriodicSync() // Y luego reprogramamos lo nuevo limpio
    }

    private fun setupPeriodicSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        // Sync cada 15 minutos (mínimo permitido por Android)
        val periodicRequest = androidx.work.PeriodicWorkRequestBuilder<com.manuelbena.synkron.data.workers.SyncWorker>(
            60, java.util.concurrent.TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        // KEEP: Si ya existe, no lo reemplaza (ahorra batería)
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SynkronPeriodicSync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
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