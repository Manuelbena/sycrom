package com.manuelbena.synkron.di.repository


import com.manuelbena.synkron.data.repository.TaskRepository
import com.manuelbena.synkron.domain.interfaces.ITaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Vincula la implementación TaskRepository a la interfaz ITaskRepository.
     * Hilt sabe cómo crear TaskRepository porque su constructor está anotado con @Inject.
     */
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepository: TaskRepository
    ): ITaskRepository


}

