package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import javax.inject.Inject

class GetTaskTodayUseCase @Inject constructor(
    private val tasksRepository: ITaskRepository
) {

    suspend operator fun invoke(): List<TaskDomain> {
        return tasksRepository.getTaskToday()
    }
}