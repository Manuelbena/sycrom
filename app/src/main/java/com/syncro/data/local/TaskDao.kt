package com.syncro.data.local

import kotlinx.coroutines.flow.Flow

// TODO: Add Room annotations
interface TaskDao {
    fun getTasks(): Flow<List<TaskEntity>>
    suspend fun insertTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)
}
