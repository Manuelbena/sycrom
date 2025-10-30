package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.domain.models.TaskDomain

interface ITaskRepository {
    fun getTaskToday() : kotlinx.coroutines.flow.Flow<List<TaskDomain>>
    suspend fun insertEvent(taskDomain: TaskDomain)
    suspend fun updateEvent(taskDomain: TaskDomain)
    suspend fun deleteEvent(taskDomain: TaskDomain)
}