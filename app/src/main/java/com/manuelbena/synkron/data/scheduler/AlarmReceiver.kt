package com.manuelbena.synkron.data.scheduler

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.manuelbena.synkron.data.local.notification.NotificationHelper
import com.manuelbena.synkron.presentation.activitys.AlarmActivity
import com.manuelbena.synkron.presentation.models.ReminderMethod
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint // <--- IMPORTANTE PARA HILT
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper // Inyectamos el helper

    @SuppressLint("Wakelock")
    override fun onReceive(context: Context, intent: Intent?) {
        val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: "Recordatorio"
        val taskId = intent?.getStringExtra("EXTRA_TASK_ID") ?: ""
        val type = intent?.getStringExtra("EXTRA_TYPE") ?: ReminderMethod.NOTIFICATION.name

        // Si es tipo NOTIFICACIÓN, usamos el helper y terminamos
        if (type.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true)) {
            notificationHelper.showStandardNotification(
                title = "Sycrom",
                message = message,
                taskId = taskId
            )
            return // No ejecutamos wakeLocks ni activity fullscreen
        }

        // --- LÓGICA DE ALARMA (FULL SCREEN) ---
        // Solo llegamos aquí si type == "ALARM"

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:AlarmWakeLock"
        )
        wakeLock.acquire(3000)

        try {
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

            // 2. Intentar abrir la activity
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: Exception) {
                // Fallback para Android 14+ background start restrictions
            }

            // 3. Notificación insistente usando el Helper para construirla
            val notification = notificationHelper.getAlarmNotificationBuilder(message, fullScreenPendingIntent)
            notification.flags = notification.flags or Notification.FLAG_INSISTENT

            val ALARM_NOTIFICATION_ID = 999
            notificationHelper.getManager().notify(ALARM_NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }
}