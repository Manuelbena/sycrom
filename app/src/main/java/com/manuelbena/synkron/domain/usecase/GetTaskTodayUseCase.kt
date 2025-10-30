package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import javax.inject.Inject

class GetTaskTodayUseCase @Inject constructor(
    private val tasksRepository: ITaskRepository
) {


    operator fun invoke(): kotlinx.coroutines.flow.Flow<List<TaskDomain>> {
        return tasksRepository.getTaskToday()
    }
}