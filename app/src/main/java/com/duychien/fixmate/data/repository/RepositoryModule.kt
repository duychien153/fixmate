package com.duychien.fixmate.data.repository

import com.duychien.fixmate.data.preferences.SettingsDataStore
import com.duychien.fixmate.data.worker.SyncScheduler
import com.duychien.fixmate.data.worker.WorkManagerSyncScheduler
import com.duychien.fixmate.domain.repository.SettingsRepository
import com.duychien.fixmate.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsDataStore): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler
}
