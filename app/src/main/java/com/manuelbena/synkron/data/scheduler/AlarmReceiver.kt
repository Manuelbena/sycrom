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
        // LOGUEAMOS TODO LO QUE ENTRE
        val action = intent?.action
        Log.d("SYCROM_DEBUG", "RECEIVER: 🔔 onReceive disparado. Action: $action")

        if (action != ALARM_ACTION) {
            Log.w("SYCROM_DEBUG", "RECEIVER: Ignorando intent porque la acción no coincide ($action)")
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:NotificationProcessingLock"
        )
        wakeLock.acquire(3000)

        try {
            val message = intent?.getStringExtra("EXTRA_MESSAGE") ?: "Recordatorio"
            val title = intent?.getStringExtra("EXTRA_TITLE") ?: "Sycrom"
            val taskId = intent?.getStringExtra("EXTRA_TASK_ID") ?: "Unknown"
            val type = intent?.getStringExtra("EXTRA_TYPE") ?: "NOTIFICATION"

            Log.d("SYCROM_DEBUG", "RECEIVER: Procesando alarma. Tipo: $type, ID Tarea: $taskId, Mensaje: $message")

            if (type.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true) || type == "NOTIFICATION") {
                Log.d("SYCROM_DEBUG", "RECEIVER: Lanzando Notificación Estándar...")
                notificationHelper.showSycromNotification(
                    title = title,
                    message = message,
                    description = intent?.getStringExtra("EXTRA_DESC") ?: "",
                    subtasks = intent?.getStringExtra("EXTRA_SUBTASKS") ?: "",
                    location = intent?.getStringExtra("EXTRA_LOCATION") ?: "",
                    taskId = taskId,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                Log.d("SYCROM_DEBUG", "RECEIVER: Lanzando Actividad de Alarma...")
                launchAlarmActivity(context, message, taskId)
            }

        } catch (e: Exception) {
            Log.e("SYCROM_DEBUG", "RECEIVER CRITICAL ERROR: ${e.message}")
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            // Usamos un requestCode único para el PendingIntent de la actividad también
            val reqCode = taskId?.hashCode() ?: 0
            val pendingIntent = PendingIntent.getActivity(
                context, reqCode, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("SYCROM_DEBUG", "RECEIVER: Iniciando Activity...")
            context.startActivity(fullScreenIntent)

            // Notificación insistente de respaldo
            val notif = notificationHelper.getAlarmNotificationBuilder(message, fullScreenPendingIntent = pendingIntent)
            notif.flags = notif.flags or Notification.FLAG_INSISTENT
            notificationHelper.getManager().notify(999, notif)

        } catch (e: Exception) {
            Log.e("SYCROM_DEBUG", "RECEIVER Error lanzando activity: ${e.message}")
            e.printStackTrace()
        }
    }
}