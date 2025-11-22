package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import javax.inject.Inject

class SendTaskIaUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    suspend operator fun invoke(message: String): Result<Boolean> {
        if (message.isBlank()) return Result.failure(Exception("El mensaje no puede estar vacío"))
        return repository.sendTaskToAi(message)
    }
}