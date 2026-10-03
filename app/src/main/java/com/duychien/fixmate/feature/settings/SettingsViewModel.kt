package com.duychien.fixmate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duychien.fixmate.core.common.AppError
import com.duychien.fixmate.core.common.AppResult
import com.duychien.fixmate.core.common.safeCall
import com.duychien.fixmate.domain.model.ThemeMode
import com.duychien.fixmate.domain.repository.SettingsRepository
import com.duychien.fixmate.domain.usecase.ObservePendingSyncCountUseCase
import com.duychien.fixmate.domain.usecase.RefreshTasksUseCase
import com.duychien.fixmate.domain.usecase.SyncTasksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lastSyncAt: Long? = null,
    val pendingSyncCount: Int = 0,
    val isSyncing: Boolean = false,
)

sealed interface SettingsEvent {
    data object SyncSucceeded : SettingsEvent
    data class SyncFailed(val error: AppError) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    observePendingSyncCount: ObservePendingSyncCountUseCase,
    private val syncTasks: SyncTasksUseCase,
    private val refreshTasks: RefreshTasksUseCase,
) : ViewModel() {

    private val isSyncing = MutableStateFlow(false)

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.lastSyncAt,
        observePendingSyncCount(),
        isSyncing,
    ) { theme, lastSync, pending, syncing ->
        SettingsUiState(
            themeMode = theme,
            lastSyncAt = lastSync,
            pendingSyncCount = pending,
            isSyncing = syncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** Pushes pending local changes first, then pulls the latest remote snapshot. */
    fun onSyncNow() {
        if (isSyncing.value) return
        viewModelScope.launch {
            isSyncing.value = true
            val result = safeCall {
                syncTasks()
                refreshTasks()
            }
            isSyncing.value = false
            when (result) {
                is AppResult.Success -> _events.send(SettingsEvent.SyncSucceeded)
                is AppResult.Error -> _events.send(SettingsEvent.SyncFailed(result.error))
            }
        }
    }
}
