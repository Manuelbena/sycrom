package com.manuelbena.synkron.di.repository

import android.content.Context
import androidx.room.Room
import com.manuelbena.synkron.data.local.SykromDataBase
import com.manuelbena.synkron.data.local.models.BudgetDao
import com.manuelbena.synkron.data.local.models.SuperTaskDao
import com.manuelbena.synkron.data.local.models.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAgendaDatabase(@ApplicationContext context: Context): SykromDataBase {
        return Room.databaseBuilder(
            context,
            SykromDataBase::class.java,
            "agenda_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideEventDao(database: SykromDataBase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideSuperTaskDao(database: SykromDataBase): SuperTaskDao {
        return database.superTaskDao()
    }

    // AQUÍ ESTÁ LA CORRECCIÓN:
    @Provides
    @Singleton
    fun provideBudgetDao(database: SykromDataBase): BudgetDao {
        return database.budgetDao() // <-- Aquí llamamos a la función abstracta de la BD
    }
}