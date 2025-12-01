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
        // Deben coincidir con App.kt
        const val ALARM_CHANNEL_ID = "ALARM_CHANNEL_V2"
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_NOTIFICATIONS_FINAL"
    }

    fun showStandardNotification(title: String, message: String, taskId: String?) {
        Log.d("SYCROM_DEBUG", "Helper: Iniciando showStandardNotification para $title")

        // Intent para abrir la app
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

        // Construcción de la notificación
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Asegúrate de que este recurso existe
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Para Android < 8.0 y compatibilidad
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // Importante para Do Not Disturb
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Visible en bloqueo
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibración forzada en el builder
            .setDefaults(Notification.DEFAULT_SOUND) // Sonido default

        val notificationId = taskId?.hashCode() ?: System.currentTimeMillis().toInt()

        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d("SYCROM_DEBUG", "Helper: notify() llamado con éxito. ID: $notificationId en Canal: $NOTIFICATION_CHANNEL_ID")
        } catch (e: Exception) {
            Log.e("SYCROM_DEBUG", "Helper: Error al llamar a notify(): ${e.message}")
            e.printStackTrace()
        }
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