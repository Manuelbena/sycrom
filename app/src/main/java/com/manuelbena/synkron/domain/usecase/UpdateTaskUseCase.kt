package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import javax.inject.Inject

/**
 * Caso de uso para actualizar una tarea existente en el repositorio.
 */
class UpdateTaskUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    /**
     * Invoca el caso de uso.
     * @param task La tarea con la información actualizada.
     */
    suspend operator fun invoke(task: TaskDomain) {
        repository.updateEvent(task)
    }
}
