package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: ITaskRepository,
    private val checkFocusUseCase: CheckFocusUseCase
) {
    suspend operator fun invoke(task: TaskDomain) {
        val oldTask = repository.getTaskById(task.id)
        repository.updateTask(task)

        if (task.isDone && oldTask?.isDone == false) {
            checkFocusUseCase(task)
        }
    }
}
