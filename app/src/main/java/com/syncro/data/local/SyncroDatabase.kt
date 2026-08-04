package com.syncro.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.syncro.data.local.dao.TaskDao
import com.syncro.data.local.entity.TaskEntity

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class SyncroDatabase : RoomDatabase() {
    abstract val taskDao: TaskDao
}
