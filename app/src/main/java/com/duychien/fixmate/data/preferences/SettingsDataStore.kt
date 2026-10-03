package com.duychien.fixmate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.duychien.fixmate.core.common.Constants
import com.duychien.fixmate.domain.model.ThemeMode
import com.duychien.fixmate.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.SETTINGS_DATASTORE_NAME,
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val dataStore get() = context.settingsDataStore

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
    }

    override val lastSyncAt: Flow<Long?> = dataStore.data.map { prefs -> prefs[Keys.LAST_SYNC_AT] }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setLastSyncAt(timestamp: Long) {
        dataStore.edit { it[Keys.LAST_SYNC_AT] = timestamp }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }
}
