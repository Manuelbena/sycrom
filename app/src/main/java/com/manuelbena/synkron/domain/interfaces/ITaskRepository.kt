package com.manuelbena.synkron.domain.interfaces

import com.manuelbena.synkron.presentation.models.TaskPresentation

interface ITaskRepository {
    suspend fun getTaskToday() : List<TaskPresentation>
}