package com.duychien.fixmate.core.database

import android.content.Context
import androidx.room.Room
import com.duychien.fixmate.core.common.Constants
import com.duychien.fixmate.core.database.dao.TaskDao
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
    fun provideDatabase(@ApplicationContext context: Context): FixMateDatabase =
        Room.databaseBuilder(context, FixMateDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideTaskDao(database: FixMateDatabase): TaskDao = database.taskDao()
}
