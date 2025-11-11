package com.manuelbena.synkron.di.repository

import android.content.Context
import androidx.room.Room
import com.manuelbena.synkron.data.local.SykromDataBase
import com.manuelbena.synkron.data.local.models.TaskDao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt para proveer dependencias de la base de datos Room.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provee la instancia única (Singleton) de la base de datos.
     */
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

    /**
     * Provee la instancia única del DAO, obteniéndola desde la BD.
     * Hilt sabe cómo crear 'SykromDataBase' gracias a la función anterior.
     */
    @Provides
    @Singleton
    fun provideEventDao(database: SykromDataBase): TaskDao {
        return database.taskDao()
    }
}
