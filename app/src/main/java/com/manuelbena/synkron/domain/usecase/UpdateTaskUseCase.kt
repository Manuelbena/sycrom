package com.manuelbena.synkron.domain.usecase

import TaskDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository

import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    // CAMBIO: Llama al nuevo método 'updateTask'
    suspend operator fun invoke(task: TaskDomain) {
        repository.updateTask(task)
    }
}