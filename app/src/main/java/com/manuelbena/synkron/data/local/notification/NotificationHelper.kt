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
        const val NOTIFICATION_CHANNEL_ID = "SYCROM_ALERTS_V12"
    }

    private val sycromPhrases = listOf(
        "Aquí tienes lo siguiente en tu agenda.",
        "Es hora de avanzar. ¿Listo?",
        "Tu asistente Synkróm te recuerda:",
        "Mantén el ritmo, tienes esto pendiente.",
        "Un pequeño recordatorio para una gran tarea."
    )
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

        // Usamos un requestCode único para evitar que los intents se sobrescriban si hay varios
        val uniqueRequestCode = taskId?.hashCode() ?: System.currentTimeMillis().toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            // 🔥 CLAVE: Estilo expandible para que se lea todo el texto del resumen/briefing
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = taskId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
    }

    fun showSycromNotification(
        title: String,
        message: String,
        description: String,
        subtasks: String,
        location: String,
        taskId: String?,
        timestamp: Long = System.currentTimeMillis()
    ) {
        Log.d("SYCROM_DEBUG", "HELPER: Construyendo Notificación para ID=$taskId")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val reviewIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId)
            putExtra("EXTRA_ACTION", "REVIEW")
        }
        val reviewPendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode() + 1, reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sycromPhrase = sycromPhrases.random()
        val stringBuilder = StringBuilder()
        stringBuilder.append(sycromPhrase).append("\n\n")
        if (location.isNotBlank()) stringBuilder.append("📍 ").append(location).append("\n")
        if (description.isNotBlank()) stringBuilder.append("📝 ").append(description).append("\n")
        if (subtasks.isNotBlank()) stringBuilder.append("\nSubtareas:\n").append(subtasks)

        val bigText = stringBuilder.toString().trim()

        // Construcción compatible con WEAR OS
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

            // CATEGORÍA IMPORTANTE: 'ALARM' o 'EVENT' suelen vibrar mejor en el reloj
            .setCategory(NotificationCompat.CATEGORY_ALARM)

            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setWhen(timestamp)
            .setShowWhen(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setDefaults(Notification.DEFAULT_SOUND)

            // Permite que se vea en otros dispositivos (Reloj, Auto)
            .setLocalOnly(false)

            .addAction(android.R.drawable.ic_menu_view, "Revisar", reviewPendingIntent)

        // Añadir WearableExtender para acciones específicas en el reloj (opcional pero recomendado)
        val wearAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Abrir en Móvil",
            pendingIntent
        ).build()

        val wearExtender = NotificationCompat.WearableExtender()
            .addAction(wearAction)

        builder.extend(wearExtender)

        val notificationId = taskId?.hashCode() ?: System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, builder.build())
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