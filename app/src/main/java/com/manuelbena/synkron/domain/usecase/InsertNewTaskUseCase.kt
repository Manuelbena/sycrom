package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import javax.inject.Inject

class InsertNewTaskUseCase @Inject constructor(
    private val tasksRepository: ITaskRepository)
    {

        suspend operator fun invoke(taskDomain: TaskDomain) {
            return tasksRepository.insertEvent(taskDomain)
        }
    }
