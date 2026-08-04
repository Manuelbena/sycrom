package com.syncro.domain.usecase

import com.syncro.domain.model.SyncroItem
import com.syncro.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<SyncroItem.Task>> {
        return repository.getTasksByDate(date)
    }
}
