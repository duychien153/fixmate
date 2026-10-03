package com.duychien.fixmate.domain.repository

import com.duychien.fixmate.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val themeMode: Flow<ThemeMode>

    /** Epoch millis of the last successful sync, or null if never synced. */
    val lastSyncAt: Flow<Long?>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLastSyncAt(timestamp: Long)
}
