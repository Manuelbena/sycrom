package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ITaskRepository {
    fun getTaskToday() : kotlinx.coroutines.flow.Flow<List<TaskDomain>>
    suspend fun insertEvent(taskDomain: TaskDomain)
    suspend fun updateEvent(taskDomain: TaskDomain)
    suspend fun deleteEvent(taskDomain: TaskDomain)

    fun getTasksForDate(date: LocalDate): Flow<List<TaskDomain>>
}