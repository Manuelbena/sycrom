package com.manuelbena.synkron.di.repository

import com.manuelbena.synkron.data.repository.TasksRepository
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
interface TaskRepositoryModule {
    @Binds
    fun bind(repository: TasksRepository): ITaskRepository
}