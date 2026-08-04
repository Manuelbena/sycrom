package com.syncro.di

import android.content.Context
import androidx.room.Room
import com.syncro.data.local.SyncroDatabase
import com.syncro.data.local.dao.TaskDao
import com.syncro.data.repository.TaskRepositoryImpl
import com.syncro.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSyncroDatabase(@ApplicationContext context: Context): SyncroDatabase {
        return Room.databaseBuilder(
            context,
            SyncroDatabase::class.java,
            "syncro_db"
        ).build()
    }

    @Provides
    fun provideTaskDao(db: SyncroDatabase): TaskDao {
        return db.taskDao
    }

    @Provides
    @Singleton
    fun provideTaskRepository(dao: TaskDao): TaskRepository {
        return TaskRepositoryImpl(dao)
    }
}
