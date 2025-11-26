package com.manuelbena.synkron.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.manuelbena.synkron.domain.models.TaskDomain
import com.manuelbena.synkron.presentation.models.ReminderMethod
import dagger.hilt.android.qualifiers.ApplicationContext


import javax.inject.Inject

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(task: TaskDomain) {
        // Solo programamos si tiene recordatorios tipo ALARMA
        val alarmReminders = task.reminders.overrides.filter {
            it.method.equals(ReminderMethod.ALARM.name, ignoreCase = true)
        }
        android.util.Log.d("SYCROM_ALARM", "Intentando programar alarma para tarea: ${task.summary}")
        android.util.Log.d("SYCROM_ALARM", "Recordatorios encontrados tipo ALARM: ${alarmReminders.size}")

        // Necesitamos la fecha de inicio de la tarea para calcular el offset
        val startMillis = task.start?.dateTime?.toInstant()?.toEpochMilli() ?: return

        alarmReminders.forEach { reminder ->
            // Calculamos el momento exacto: Inicio - Antelación (minutos)
            val triggerTime = startMillis - (reminder.minutes * 60 * 1000)

            // Validar que no sea en el pasado
            if (triggerTime > System.currentTimeMillis()) {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("EXTRA_MESSAGE", reminder.message ?: task.summary)
                    putExtra("EXTRA_TASK_ID", task.id.toString())

                }

                // Usamos hashCode del ID + minutos para que sea único por recordatorio
                val requestCode = task.id.hashCode() + reminder.minutes

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // CAMBIO: Usar AlarmClockInfo en lugar de setExactAndAllowWhileIdle
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

                // Log para confirmar
                android.util.Log.d("SYCROM_ALARM", "Alarma programada modo RELOJ (Máxima prioridad) a las $triggerTime")

                // Programar alarma exacta
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    android.util.Log.d("SYCROM_ALARM", "¡Alarma fijada en AlarmManager con éxito!")
                } catch (e: SecurityException) {
                    // Aquí deberías manejar si el usuario no dio permiso de Alarmas Exactas (Android 12+)
                    e.printStackTrace()
                }
            }else{
                android.util.Log.e("SYCROM_ALARM", "ERROR: La hora es en el pasado, no se programó.")
            }
        }
    }

    // Función para cancelar (opcional pero recomendada al borrar tarea)
    fun cancel(task: TaskDomain) {
        // Lógica similar para recrear los PendingIntent y llamar a alarmManager.cancel()
    }
}