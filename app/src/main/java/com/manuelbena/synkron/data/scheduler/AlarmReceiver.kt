package com.manuelbena.synkron.data.scheduler

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import android.media.RingtoneManager
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.presentation.activitys.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("Wakelock")
    override fun onReceive(context: Context, intent: Intent?) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:AlarmWakeLock"
        )
        wakeLock.acquire(3000)

        try {
            val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: "Alarma"
            val taskId = intent?.getStringExtra("EXTRA_TASK_ID") ?: ""

            // 1. Intent para la Activity (Pantalla completa)
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("EXTRA_MESSAGE", message)
                putExtra("EXTRA_TASK_ID", taskId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                taskId.hashCode(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 2. Intentar abrir la activity manualmente (Funciona si el móvil está en uso)
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: Exception) {
                // En Android 14 segundo plano esto fallará, es normal.
            }

            // 3. Construir la Notificación CON SONIDO (El respaldo obligatorio)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "ALARM_CHANNEL_V2" // Asegúrate de que coincida con App.kt

            // Sonido de alarma real
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Sycrom")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Esto es lo que intenta abrir la pantalla en bloqueo:
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                // ¡IMPORTANTE! Devolvemos el sonido a la notificación por si la activity falla
                .setSound(null)
                .build()

            // Hacer que suene en bucle hasta que se atienda
            notification.flags = notification.flags or Notification.FLAG_INSISTENT

            val ALARM_NOTIFICATION_ID = 999
            notificationManager.notify(ALARM_NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}