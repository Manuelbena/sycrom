package com.manuelbena.synkron.data.scheduler

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.manuelbena.synkron.R
// Asegúrate de que este import coincida con donde creaste tu AlarmActivity
import com.manuelbena.synkron.presentation.alarm.AlarmActivity

class AlarmReceiver : BroadcastReceiver() {

    @SuppressLint("Wakelock")
    override fun onReceive(context: Context, intent: Intent?) {
        // 1. ADQUIRIR WAKELOCK: "Seguro de vida" para que el móvil no se duerma
        // mientras procesamos la alarma.
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:AlarmWakeLock"
        )
        // Mantenemos el procesador despierto 3 segundos, suficiente para abrir la Activity
        wakeLock.acquire(3000)

        try {
            val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: "Alarma"
            val taskId = intent?.getStringExtra("EXTRA_TASK_ID") ?: ""

            // 2. Crear el Intent para lanzar la ACTIVIDAD a pantalla completa
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("EXTRA_MESSAGE", message)
                putExtra("EXTRA_TASK_ID", taskId)
                // Flags OBLIGATORIOS para iniciar una Activity desde un Receiver
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

            // 3. Configurar la Notificación
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // NOTA: Usamos "ALARM_CHANNEL_V2" para asegurarnos de que es un canal limpio.
            // (Recuerda actualizar el ID también en tu App.kt si no lo has hecho)
            val channelId = "ALARM_CHANNEL_V2"

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Sycrom")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // AQUÍ ESTÁ LA MAGIA: Esto lanza la Activity si el móvil está bloqueado
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                // Silenciamos la notificación para que sea la Activity la que toque el sonido
                // (Evita doble sonido o conflictos)
                .setSound(null)

            val notification = notificationBuilder.build()

            // Flag para que la notificación sea "insistente" en la barra de estado
            // (por si la activity fallara en abrirse)
            notification.flags = notification.flags or Notification.FLAG_INSISTENT

            notificationManager.notify(taskId.hashCode(), notification)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Liberamos el bloqueo del procesador
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}