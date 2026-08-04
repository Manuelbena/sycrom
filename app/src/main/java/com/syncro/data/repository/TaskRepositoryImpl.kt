package com.syncro.data.repository

import com.syncro.data.local.dao.TaskDao
import com.syncro.data.local.entity.TaskEntity
import com.syncro.domain.model.SyncroItem
import com.syncro.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {

    override fun getTasksByDate(date: LocalDate): Flow<List<SyncroItem.Task>> {
        val epochDay = date.toEpochDay()
        return dao.getTasksByDate(epochDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertTask(task: SyncroItem.Task, description: String, date: LocalDate) {
        dao.insertTask(
            TaskEntity(
                title = task.title,
                description = description,
                date = date.toEpochDay(),
                time = task.time,
                isCompleted = task.isCompleted
            )
        )
    }

    override suspend fun toggleTaskCompletion(taskId: String) {
        val id = taskId.toIntOrNull() ?: return
        dao.getTaskById(id)?.let { entity ->
            dao.updateTask(entity.copy(isCompleted = !entity.isCompleted))
        }
    }

    private fun TaskEntity.toDomain(): SyncroItem.Task {
        return SyncroItem.Task(
            id = id.toString(),
            title = title,
            description = description,
            time = time,
            isCompleted = isCompleted
        )
    }
}
