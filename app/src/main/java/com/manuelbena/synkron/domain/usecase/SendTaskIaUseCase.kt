// Archivo: app/src/main/java/com/manuelbena/synkron/domain/usecase/SendTaskIaUseCase.kt

package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.data.remote.n8n.models.N8nChatResponse
import com.manuelbena.synkron.domain.models.TaskDomain
import okhttp3.internal.concurrent.Task
import javax.inject.Inject

class SendTaskIaUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    suspend operator fun invoke(message: String): Result<TaskDomain> {
        // Aquí podrías añadir lógica extra antes de enviar (ej. validar mensaje vacío)
        if (message.isBlank()) return Result.failure(Exception("El mensaje no puede estar vacío"))

        return repository.sendIaMessage(message)
    }
}