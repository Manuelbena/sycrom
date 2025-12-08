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
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.presentation.activitys.AlarmActivity
import com.manuelbena.synkron.presentation.models.ReminderMethod
import com.manuelbena.synkron.presentation.util.toHourString
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var repository: ITaskRepository      // 🔥 Faltaba inyectar esto
    @Inject lateinit var briefingScheduler: BriefingScheduler // 🔥 Faltaba inyectar esto

    companion object {
        const val ALARM_ACTION = "com.manuelbena.synkron.ACTION_ALARM_TRIGGER"

        // 🔥 ESTAS SON LAS CONSTANTES QUE FALTABAN:
        const val TYPE_MORNING_BRIEFING = "TYPE_MORNING_BRIEFING"
        const val TYPE_EVENING_DEBRIEF = "TYPE_EVENING_DEBRIEF"
    }

    @SuppressLint("Wakelock")
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("SYCROM_DEBUG", "RECEIVER: 🔔 onReceive disparado. Action: $action")

        // Filtro de seguridad: Solo aceptamos nuestra acción
        if (action != ALARM_ACTION) {
            Log.w("SYCROM_DEBUG", "RECEIVER: Ignorando intent porque la acción no coincide ($action)")
            return
        }

        // Obtenemos el tipo de alarma
        val type = intent.getStringExtra("EXTRA_TYPE") ?: "NOTIFICATION"

        // 1. LÓGICA DE REPORTES DIARIOS (BRIEFING/DEBRIEF)
        if (type == TYPE_MORNING_BRIEFING || type == TYPE_EVENING_DEBRIEF) {
            Log.d("SYCROM_DEBUG", "RECEIVER: Procesando reporte diario: $type")
            handleDailyReports(type)

            // 🔥 REPROGRAMACIÓN AUTOMÁTICA (Bucle Infinito para mañana)
            if (type == TYPE_MORNING_BRIEFING) {
                briefingScheduler.scheduleMorningBriefing()
            } else {
                briefingScheduler.scheduleEveningDebrief()
            }
            return
        }

        // 2. LÓGICA ESTÁNDAR: Recordatorios de Tareas
        processTaskAlarm(context, intent, type)
    }

    private fun processTaskAlarm(context: Context, intent: Intent, type: String) {
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Recordatorio"
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Sycrom"
        val taskId = intent.getStringExtra("EXTRA_TASK_ID") ?: ""

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sycrom:NotificationProcessingLock"
        )
        wakeLock.acquire(3000)

        try {
            if (type.equals(ReminderMethod.NOTIFICATION.name, ignoreCase = true) || type == "NOTIFICATION") {
                notificationHelper.showSycromNotification(
                    title = title,
                    message = message,
                    description = intent.getStringExtra("EXTRA_DESC") ?: "",
                    subtasks = intent.getStringExtra("EXTRA_SUBTASKS") ?: "",
                    location = intent.getStringExtra("EXTRA_LOCATION") ?: "",
                    taskId = taskId,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                launchAlarmActivity(context, message, taskId)
            }
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun handleDailyReports(type: String) {
        val pendingResult = goAsync() // Mantiene vivo el BroadcastReceiver
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val today = LocalDate.now()
                val tasks = repository.getTasksForDate(today).first()

                if (type == TYPE_MORNING_BRIEFING) {
                    if (tasks.isNotEmpty()) {
                        val taskCount = tasks.size
                        // Filtro seguro para evitar crash con nulos
                        val lastTaskEnd = tasks.mapNotNull { it.end }
                            .filter { it.dateTime != null }
                            .maxByOrNull { it.dateTime!! }

                        val endTimeString = lastTaskEnd?.toHourString() ?: "??:??"

                        notificationHelper.showStandardNotification(
                            "☀️ Buenos días, Manuel",
                            "Tienes $taskCount misiones hoy 🚀. Terminas sobre las $endTimeString 🏁.",
                            null
                        )
                    } else {
                        notificationHelper.showStandardNotification("☀️ Agenda libre", "Hoy no tienes misiones. ¿Planificamos algo? 🤔", null)
                    }
                } else {
                    // DEBRIEF DE LA NOCHE
                    if (tasks.isNotEmpty()) {
                        val done = tasks.count { it.isDone }
                        val total = tasks.size
                        val pct = if (total > 0) (done * 100) / total else 0
                        val emoji = if (pct >= 80) "🔥" else "⚡"
                        notificationHelper.showStandardNotification(
                            "$emoji Resumen: $pct% Completado",
                            "Te quedan ${total - done} pendientes. ¡Descansa! 💤",
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun launchAlarmActivity(context: Context, message: String, taskId: String?) {
        try {
            val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("EXTRA_MESSAGE", message)
                putExtra("EXTRA_TASK_ID", taskId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            val reqCode = taskId?.hashCode() ?: 0
            val pendingIntent = PendingIntent.getActivity(
                context, reqCode, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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