package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.domain.models.SuperTaskModel
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ISuperTaskRepository {
    fun getSuperTasksForDate(date: LocalDate): Flow<List<SuperTaskModel>>

    fun getAllSuperTasksForDate(): Flow<List<SuperTaskModel>>
    suspend fun saveSuperTask(task: SuperTaskModel)
    suspend fun deleteSuperTask(task: SuperTaskModel)
}