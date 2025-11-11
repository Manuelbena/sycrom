package com.manuelbena.synkron.domain.usecase


import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain

import javax.inject.Inject

class InsertNewTaskUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    // CAMBIO: Llama al nuevo método 'insertTask'
    suspend operator fun invoke(task: TaskDomain) {
        repository.insertTask(task)
    }
}