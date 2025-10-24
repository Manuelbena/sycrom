package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.presentation.models.TaskPresentation
import javax.inject.Inject

class GetTaskTodayUseCase @Inject constructor(
    private val tasksRepository: ITaskRepository
) {

    suspend operator fun invoke(): List<TaskPresentation> {
        return tasksRepository.getTaskToday()
    }
}