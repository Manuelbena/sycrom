package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.manuelbena.synkron.domain.models.TaskDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    fun schedule(task: TaskDomain) {
        // Log inicial para saber que entramos
        Log.d("SYCROM_DEBUG", "SCHEDULER: -----------------------------------------")
        Log.d("SYCROM_DEBUG", "SCHEDULER: Intentando programar para Tarea ID=${task.id} - ${task.summary}")

        val allReminders = task.reminders.overrides
        if (allReminders.isEmpty()) {
            Log.d("SYCROM_DEBUG", "SCHEDULER: La tarea no tiene recordatorios. Saliendo.")
            return
        }

        // Validación de fecha
        val startMillis = task.start?.dateTime
        if (startMillis == null) {
            Log.e("SYCROM_DEBUG", "SCHEDULER ERROR: La tarea no tiene fecha de inicio (startMillis es null).")
            return
        }

        Log.d("SYCROM_DEBUG", "SCHEDULER: Fecha Inicio Tarea = ${sdf.format(Date(startMillis))}")

        val remindersToSchedule = allReminders.filter { reminder ->
            val method = reminder.method.trim().lowercase()
            method.contains("alarm") || method.contains("notif") || method.contains("popup")
        }

        val subtasksSummary = if (task.subTasks.isNotEmpty()) {
            task.subTasks.joinToString("\n") { "• ${it.title}" }
        } else {
            ""
        }

        remindersToSchedule.forEach { reminder ->
            // Cálculo del tiempo
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)
            val now = System.currentTimeMillis()

            // LOG CLAVE: ¿Cuándo va a sonar?
            val triggerDate = Date(triggerTime)
            Log.d("SYCROM_DEBUG", "SCHEDULER: --- Procesando Recordatorio (${reminder.minutes} min antes) ---")
            Log.d("SYCROM_DEBUG", "SCHEDULER: Hora Objetivo de Alarma: ${sdf.format(triggerDate)}")
            Log.d("SYCROM_DEBUG", "SCHEDULER: Hora Actual del Sistema: ${sdf.format(Date(now))}")

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ALARM_ACTION
                putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                putExtra("EXTRA_TITLE", task.summary)
                putExtra("EXTRA_DESC", task.description ?: "")
                putExtra("EXTRA_SUBTASKS", subtasksSummary)
                putExtra("EXTRA_LOCATION", task.location ?: "")
                putExtra("EXTRA_TASK_ID", task.id.toString())

                val method = reminder.method.lowercase()
                val typeToSend = if (method.contains("alarm")) "ALARM" else "NOTIFICATION"
                putExtra("EXTRA_TYPE", typeToSend)
                putExtra("EXTRA_TIME", triggerTime)
            }

            // Usamos un RequestCode único
            val requestCode = task.id.hashCode() + reminder.minutes

            if (triggerTime > now) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Usamos setAlarmClock
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                Log.d("SYCROM_DEBUG", "SCHEDULER: ✅ ALARMA PROGRAMADA EXITOSAMENTE (A futuro)")
            } else {
                // Lógica de "hace poco" para disparar al momento si acabas de crear una tarea pasada
                // Aumentamos el margen a 1 hora para pruebas
                val marginTime = now - (60 * 60 * 1000)

                if (triggerTime > marginTime) {
                    Log.w("SYCROM_DEBUG", "SCHEDULER: ⚠️ La hora ya pasó, pero hace poco. Disparando INMEDIATO.")
                    context.sendBroadcast(intent)
                } else {
                    Log.e("SYCROM_DEBUG", "SCHEDULER: ❌ Omitida. Es demasiado antigua (${sdf.format(triggerDate)})")
                }
            }
        }
        Log.d("SYCROM_DEBUG", "SCHEDULER: -----------------------------------------")
    }

    fun cancel(task: TaskDomain) {
        Log.d("SYCROM_DEBUG", "SCHEDULER: Cancelando alarmas para tarea ID=${task.id}")
        try {
            task.reminders.overrides.forEach { reminder ->
                val requestCode = task.id.hashCode() + reminder.minutes
                val intent = Intent(context, AlarmReceiver::class.java)
                intent.action = AlarmReceiver.ALARM_ACTION
                val pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}