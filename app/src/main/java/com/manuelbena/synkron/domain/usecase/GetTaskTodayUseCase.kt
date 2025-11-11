package com.manuelbena.synkron.domain.usecase

import TaskDomain
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetTaskTodayUseCase @Inject constructor(
    private val repository: ITaskRepository
) {
    // CAMBIO: Ya no es 'suspend' y acepta LocalDate
    operator fun invoke(date: LocalDate): Flow<List<TaskDomain>> {
        return repository.getTasksForDate(date)
    }
}