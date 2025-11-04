package com.manuelbena.synkron.domain.usecase

import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import com.manuelbena.synkron.domain.models.TaskDomain
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

/**
 * Caso de uso para OBTENER las tareas de una fecha específica.
 */
class GetTasksForDateUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    /**
     * Invoca el caso de uso.
     * @param date El día específico del que se quieren obtener las tareas.
     */
    operator fun invoke(date: LocalDate): Flow<List<TaskDomain>> {
        return repository.getTasksForDate(date)
    }
}
