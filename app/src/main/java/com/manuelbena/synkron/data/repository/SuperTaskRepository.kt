package com.manuelbena.synkron.data.repository

import com.manuelbena.synkron.data.local.entities.toDomain
import com.manuelbena.synkron.data.local.entities.toEntity
import com.manuelbena.synkron.data.local.models.SuperTaskDao
import com.manuelbena.synkron.domain.interfaces.ISuperTaskRepository
import com.manuelbena.synkron.domain.models.SuperTaskModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class SuperTaskRepository @Inject constructor(
    private val dao: SuperTaskDao
) : ISuperTaskRepository {

    override fun getSuperTasksForDate(date: LocalDate): Flow<List<SuperTaskModel>> {
        val zoneId = ZoneId.systemDefault()
        val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        return dao.getSuperTasksForDate(startOfDay, endOfDay).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveSuperTask(task: SuperTaskModel) {
        dao.insertSuperTask(task.toEntity())
    }

    override suspend fun deleteSuperTask(task: SuperTaskModel) {
        dao.deleteSuperTask(task.toEntity())
    }
}