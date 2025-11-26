package com.manuelbena.synkron.data.scheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.manuelbena.synkron.R
import com.manuelbena.synkron.presentation.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: "Alarma"
        val taskId = intent?.getStringExtra("EXTRA_TASK_ID") ?: ""

        // 1. Crear el Intent para TU NUEVA ACTIVITY
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra("EXTRA_MESSAGE", message)
            putExtra("EXTRA_TASK_ID", taskId)
            // Estos flags son obligatorios para iniciar una Activity desde un Receiver
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Crear la notificación con prioridad MÁXIMA y fullScreenIntent
        // NOTA: Aunque lanzamos la Activity, la notificación es necesaria para que Android permita la interrupción.
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationBuilder = NotificationCompat.Builder(context, "ALARM_CHANNEL_HIGH")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Sycrom")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // EL TRUCO ESTÁ AQUÍ:
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            // Poner sonido nulo en la notificación para que lo maneje la Activity (opcional,
            // pero mejor dejar que la Activity toque la música para tener control del botón "Parar")
            .setSound(null)

        notificationManager.notify(taskId.hashCode(), notificationBuilder.build())
    }
}