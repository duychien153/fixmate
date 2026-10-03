package com.duychien.fixmate.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.duychien.fixmate.core.database.dao.TaskDao
import com.duychien.fixmate.core.database.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class FixMateDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
