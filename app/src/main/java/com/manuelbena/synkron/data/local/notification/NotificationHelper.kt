package com.manuelbena.synkron.data.local.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.presentation.activitys.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        // CAMBIA ESTO: Debe coincidir exactamente con App.kt
        const val NOTIFICATION_CHANNEL_ID = "TASK_NOTIFICATION_CHANNEL_V5"
        const val ALARM_CHANNEL_NAME = "Alarmas Prioritarias"
        const val NOTIFICATION_CHANNEL_NAME = "Recordatorios de Tareas"
    }

    // (El init y createNotificationChannels se pueden omitir aquí si ya están en App.kt, pero no estorban)

    fun showStandardNotification(title: String, message: String, taskId: String?) {
        Log.d("SYCROM_DEBUG", "NotificationHelper: Construyendo notificación para $title")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // IMPORTANTE: Asegúrate de que el ID del canal coincida con el de App.kt
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            // --- CONFIGURACIÓN PARA QUE "SALTE" (HEADS-UP) ---
            .setPriority(NotificationCompat.PRIORITY_HIGH) // <--- ¡CAMBIADO A HIGH!
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // <--- AYUDA AL SISTEMA
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // <--- VISIBLE EN BLOQUEO
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val notificationId = taskId?.hashCode() ?: System.currentTimeMillis().toInt()

        Log.d("SYCROM_DEBUG", "NotificationHelper: Enviando notificación ID: $notificationId al canal $NOTIFICATION_CHANNEL_ID")
        notificationManager.notify(notificationId, notification)
    }

    fun getAlarmNotificationBuilder(message: String, fullScreenPendingIntent: PendingIntent): Notification {
        return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Sycrom - ¡Es hora!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .setSound(null)
            .build()
    }

    fun getManager() = notificationManager
}