package com.syncro.domain.usecase

import com.syncro.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletionUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: String) {
        repository.toggleTaskCompletion(taskId)
    }
}
