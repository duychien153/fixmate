package com.duychien.fixmate.feature.tasklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duychien.fixmate.core.common.AppError
import com.duychien.fixmate.core.common.AppResult
import com.duychien.fixmate.core.common.onError
import com.duychien.fixmate.core.common.safeCall
import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.usecase.GetTasksUseCase
import com.duychien.fixmate.domain.usecase.ObservePendingSyncCountUseCase
import com.duychien.fixmate.domain.usecase.RefreshTasksUseCase
import com.duychien.fixmate.domain.usecase.ToggleTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskListViewModel @Inject constructor(
    getTasks: GetTasksUseCase,
    observePendingSyncCount: ObservePendingSyncCountUseCase,
    private val refreshTasks: RefreshTasksUseCase,
    private val toggleTask: ToggleTaskUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TaskFilter.ALL)
    private val loadState = MutableStateFlow(LoadState(isInitialLoading = true))

    private val filterState = combine(query, filter) { q, f -> FilterState(q, f) }

    val uiState: StateFlow<TaskListUiState> = combine(
        getTasks(),
        filterState,
        loadState,
        observePendingSyncCount(),
    ) { tasks, filters, load, pendingCount ->
        TaskListUiState(
            tasks = tasks.applyFilters(filters),
            query = filters.query,
            filter = filters.filter,
            // Cached data should never be hidden behind a spinner.
            isLoading = load.isInitialLoading && tasks.isEmpty(),
            isRefreshing = load.isRefreshing,
            pendingSyncCount = pendingCount,
            error = load.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskListUiState(isLoading = true),
    )

    init {
        refresh(initial = true)
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: TaskFilter) {
        filter.value = value
    }

    fun refresh() = refresh(initial = false)

    fun onToggleTask(id: Long) {
        viewModelScope.launch {
            safeCall { toggleTask(id) }.onError { error -> loadState.update { it.copy(error = error) } }
        }
    }

    fun onErrorShown() {
        loadState.update { it.copy(error = null) }
    }

    private fun refresh(initial: Boolean) {
        viewModelScope.launch {
            loadState.update { it.copy(isRefreshing = !initial, error = null) }
            val result = safeCall { refreshTasks() }
            loadState.update {
                it.copy(
                    isInitialLoading = false,
                    isRefreshing = false,
                    error = (result as? AppResult.Error)?.error,
                )
            }
        }
    }

    private fun List<Task>.applyFilters(filters: FilterState): List<Task> {
        val normalizedQuery = filters.query.trim()
        return filter { task ->
            filters.filter.matches(task) &&
                (normalizedQuery.isEmpty() || task.title.contains(normalizedQuery, ignoreCase = true))
        }
    }

    private data class FilterState(val query: String, val filter: TaskFilter)

    private data class LoadState(
        val isInitialLoading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: AppError? = null,
    )
}
