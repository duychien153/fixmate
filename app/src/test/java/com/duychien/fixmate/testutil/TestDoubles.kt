package com.duychien.fixmate.testutil

import com.duychien.fixmate.core.common.Clock
import com.duychien.fixmate.core.common.DispatcherProvider
import com.duychien.fixmate.data.worker.SyncScheduler
import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.model.ThemeMode
import com.duychien.fixmate.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class TestDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
}

class FakeClock(var current: Long = 1_700_000_000_000L) : Clock {
    override fun now(): Long = current
    fun advance(millis: Long) {
        current += millis
    }
}

class FakeSyncScheduler : SyncScheduler {
    var requestCount = 0
        private set

    override fun requestSync() {
        requestCount++
    }
}

class FakeSettingsRepository : SettingsRepository {
    private val theme = MutableStateFlow(ThemeMode.SYSTEM)
    private val lastSync = MutableStateFlow<Long?>(null)

    override val themeMode: Flow<ThemeMode> = theme
    override val lastSyncAt: Flow<Long?> = lastSync

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }

    override suspend fun setLastSyncAt(timestamp: Long) {
        lastSync.value = timestamp
    }
}

fun task(
    id: Long,
    title: String = "Task $id",
    completed: Boolean = false,
    syncState: SyncState = SyncState.SYNCED,
    updatedAt: Long = id,
): Task = Task(
    id = id,
    title = title,
    completed = completed,
    userId = 1L,
    updatedAt = updatedAt,
    syncState = syncState,
)
