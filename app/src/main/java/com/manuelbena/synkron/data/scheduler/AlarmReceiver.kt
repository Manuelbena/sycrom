package com.manuelbena.synkron.data.scheduler

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.manuelbena.synkron.data.local.notification.NotificationHelper
import com.manuelbena.synkron.presentation.activitys.AlarmActivity
import com.manuelbena.synkron.presentation.models.ReminderMethod
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    companion object {
        const val ALARM_ACTION = "com.manuelbena.synkron.ACTION_ALARM_TRIGGER"
    }

    @SuppressLint("Wakelock")
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("SYCROM_DEBUG", "RECEIVER: Recibido evento con acción: $action")

        if (action != ALARM_ACTION) {
            Log.d("SYCROM_DEBUG", "RECEIVER: Evento ignorado (no es ACTION_ALARM_TRIGGER)")
            return
        }

        // --- TRUCO PARA VIVO/OPPO: WAKELOCK SIEMPRE ---
        // Adquirimos un WakeLock parcial para asegurar que la CPU procese la notificación
        // incluso si el móvil está en sueño profundo (Doze mode).
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:NotificationProcessingLock"
        )
        // Lo mantenemos solo 3 segundos, suficiente para procesar
        wakeLock.acquire(3000)

        try {
            val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Recordatorio"
            val taskId = intent.getStringExtra("EXTRA_TASK_ID") ?: ""
            val type = intent.getStringExtra("EXTRA_TYPE") ?: ReminderMethod.NOTIFICATION.name

            Log.d("SYCROM_DEBUG", "RECEIVER: Tipo $type - Mensaje $message")

            if (type.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true)) {
                // Notificación normal
                notificationHelper.showStandardNotification(
                    title = "Sycrom",
                    message = message,
                    taskId = taskId
                )
            } else {
                // Alarma Pantalla Completa
                launchAlarmActivity(context, message, taskId)
            }

        } catch (e: Exception) {
            Log.e("SYCROM_DEBUG", "RECEIVER: Error procesando alarma: ${e.message}")
            e.printStackTrace()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun launchAlarmActivity(context: Context, message: String, taskId: String?) {
        try {
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

            // Intentar abrir la actividad
            try {
                context.startActivity(fullScreenIntent)
            } catch (e: Exception) {
                Log.e("SYCROM_DEBUG", "Error iniciando Activity de alarma: ${e.message}")
            }

            // Mostrar notificación de respaldo con FLAG_INSISTENT
            val notification = notificationHelper.getAlarmNotificationBuilder(message, fullScreenPendingIntent)
            notification.flags = notification.flags or Notification.FLAG_INSISTENT

            val ALARM_NOTIFICATION_ID = 999
            notificationHelper.getManager().notify(ALARM_NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}