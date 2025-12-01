package com.manuelbena.synkron.data.local.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
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
        const val NOTIFICATION_CHANNEL_ID = "TASK_NOTIFICATION_CHANNEL"
        const val ALARM_CHANNEL_NAME = "Alarmas Prioritarias"
        const val NOTIFICATION_CHANNEL_NAME = "Recordatorios de Tareas"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // Canal para Alarmas (Prioridad Máxima, Sonido, Override DND)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                ALARM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para alarmas de pantalla completa"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                enableVibration(true)
            }

            // Canal para Notificaciones Estándar (Prioridad Default, Sonido de notificación)
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones estándar para tareas"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(listOf(alarmChannel, notificationChannel))
        }
    }

    /**
     * Muestra una notificación estándar (Push Local).
     * Reutilizable desde cualquier parte de la app.
     */
    fun showStandardNotification(title: String, message: String, taskId: String?) {
        // Intent para abrir la app al tocar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId) // Por si quieres navegar al detalle
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // <--- CAMBIAR A HIGH
            .setCategory(NotificationCompat.CATEGORY_REMINDER) // <--- AÑADIR ESTO (Ayuda al sistema a priorizar)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // <--- AÑADIR ESTO (Para verla en bloqueo)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = taskId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Devuelve el Builder para la ALARMA (Pantalla completa), usado por el Receiver si falla la Activity.
     */
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
            .setSound(null) // El sonido lo maneja la Activity o el Ringtone manager aparte
            .build()
    }

    fun getManager() = notificationManager
}