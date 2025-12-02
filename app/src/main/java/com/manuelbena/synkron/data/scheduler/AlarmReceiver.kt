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
        if (intent?.action != ALARM_ACTION) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:NotificationProcessingLock"
        )
        wakeLock.acquire(3000)

        try {
            val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Recordatorio"
            val title = intent.getStringExtra("EXTRA_TITLE") ?: "Sycrom"
            val desc = intent.getStringExtra("EXTRA_DESC") ?: ""
            val subtasks = intent.getStringExtra("EXTRA_SUBTASKS") ?: ""
            val location = intent.getStringExtra("EXTRA_LOCATION") ?: ""

            val taskId = intent.getStringExtra("EXTRA_TASK_ID") ?: ""
            val type = intent.getStringExtra("EXTRA_TYPE") ?: ReminderMethod.NOTIFICATION.name
            val scheduledTime = intent.getLongExtra("EXTRA_TIME", System.currentTimeMillis())

            Log.d("SYCROM_DEBUG", "RECEIVER: Ejecutando $type para ID=$taskId")

            if (type.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true)) {
                notificationHelper.showSycromNotification(
                    title = title,
                    message = message, // El mensaje del recordatorio (ej: "10 min antes")
                    description = desc,
                    subtasks = subtasks,
                    location = location,
                    taskId = taskId,
                    timestamp = scheduledTime
                )
            } else {
                launchAlarmActivity(context, message, taskId)
            }

        } catch (e: Exception) {
            Log.e("SYCROM_DEBUG", "RECEIVER Error: ${e.message}")
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
            val pendingIntent = PendingIntent.getActivity(
                context, taskId.hashCode(), fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            context.startActivity(fullScreenIntent)

            val notif = notificationHelper.getAlarmNotificationBuilder(message, fullScreenPendingIntent = pendingIntent)
            notif.flags = notif.flags or Notification.FLAG_INSISTENT
            notificationHelper.getManager().notify(999, notif)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}