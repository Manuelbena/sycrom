package com.manuelbena.synkron.di.repository


import com.manuelbena.synkron.data.repository.TasksRepository
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
     * Vincula la implementación TasksRepository a la interfaz ITaskRepository.
     * Hilt sabe cómo crear TasksRepository porque su constructor está anotado con @Inject.
     * (@Binds es más eficiente que @Provides para este caso).
     */
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        tasksRepository: TasksRepository
    ): ITaskRepository
}
