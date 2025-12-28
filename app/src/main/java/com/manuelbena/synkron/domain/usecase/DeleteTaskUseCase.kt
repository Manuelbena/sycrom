package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    // CAMBIO: Ya no es 'suspend' y acepta LocalDate
    suspend operator fun invoke(task: TaskDomain) {
        repository.deleteTask(task)
    }

    // 👇 NUEVO: Borrar solo esta instancia
    suspend fun deleteInstance(task: TaskDomain) {
        repository.deleteTaskInstance(task)
    }

    // 👇 NUEVO: Borrar toda la serie futura
    suspend fun deleteSeries(task: TaskDomain) {
        repository.deleteTaskSeries(task)
    }
}

