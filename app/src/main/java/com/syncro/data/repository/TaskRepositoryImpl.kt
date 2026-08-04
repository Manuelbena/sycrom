package com.syncro.data.repository

import com.syncro.domain.model.Task
import com.syncro.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    // private val dao: TaskDao,
    // private val api: GeminiApi
) : TaskRepository {
    override fun getTasks(): Flow<List<Task>> {
        return flowOf(emptyList()) // Placeholder
    }

    override suspend fun addTask(task: Task) {
        // TODO: Implement
    }

    override suspend fun deleteTask(task: Task) {
        // TODO: Implement
    }
}
