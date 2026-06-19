package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.data.local.entities.AIAssistantInsightEntity
import com.manuelbena.synkron.data.local.models.AIAssistantDao
import com.manuelbena.synkron.data.local.models.TaskDao
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class CheckFocusUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val aiAssistantDao: AIAssistantDao
) {
    suspend operator fun invoke(completedTask: TaskDomain) {
        val now = Calendar.getInstance()
        val startOfDay = now.apply { 
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val endOfDay = now.apply { 
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        // Log para depuración
        android.util.Log.d("SYCROM_DEBUG", "CheckFocus: Tarea completada '${completedTask.summary}' con prioridad '${completedTask.priority}'")

        if (completedTask.priority.equals("BAJA", ignoreCase = true) || completedTask.priority.equals("MEDIA", ignoreCase = true)) {
            // Buscamos si hay tareas críticas para hoy aún pendientes
            val tasksToday = taskDao.getTasksBetweenDates(startOfDay, endOfDay).first()
            
            val criticalPendingToday = tasksToday.filter { 
                !it.isDone && it.priority.equals("ALTA", ignoreCase = true) 
            }

            android.util.Log.d("SYCROM_DEBUG", "CheckFocus: Tareas pendientes críticas hoy: ${criticalPendingToday.size}")

            if (criticalPendingToday.isNotEmpty()) {
                val message = "Buen trabajo con '${completedTask.summary}', pero recuerda que aún tienes objetivos críticos para hoy. ¡No pierdas el foco!"
                val insight = AIAssistantInsightEntity(
                    message = message,
                    type = "FOCO",
                    date = System.currentTimeMillis()
                )
                aiAssistantDao.insertInsight(insight)
                android.util.Log.d("SYCROM_DEBUG", "CheckFocus: Insight insertado")
            }
        }
    }
}
