package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.data.local.entities.AIAssistantInsightEntity
import com.manuelbena.synkron.data.local.models.AIAssistantDao
import com.manuelbena.synkron.data.local.models.TaskDao
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class AnalyzeWeeklyProgressUseCase @Inject constructor(
    private val taskDao: TaskDao,
    private val aiAssistantDao: AIAssistantDao
) {
    suspend operator fun invoke() {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis
        
        // Inicio de hace 7 días (00:00:00)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val lastWeekStart = calendar.timeInMillis
        
        android.util.Log.d("SYCROM_DEBUG", "AnalyzeWeekly: Buscando tareas entre $lastWeekStart y $now")

        val tasksInRange = taskDao.getTasksBetweenDates(lastWeekStart, now).first()
        
        val pendingCriticalTasks = tasksInRange.filter { 
            !it.isDone && it.priority.equals("ALTA", ignoreCase = true) 
        }

        android.util.Log.d("SYCROM_DEBUG", "AnalyzeWeekly: Tareas críticas pendientes encontradas: ${pendingCriticalTasks.size}")

        if (pendingCriticalTasks.isNotEmpty()) {
            val message = "He detectado ${pendingCriticalTasks.size} tareas críticas que se te han escapado esta semana. ¿Quieres que las reprogramemos para los huecos libres de mañana?"
            val insight = AIAssistantInsightEntity(
                message = message,
                type = "AUDITORIA",
                date = now
            )
            aiAssistantDao.insertInsight(insight)
        }
    }
}
