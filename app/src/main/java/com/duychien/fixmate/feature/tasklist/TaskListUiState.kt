package com.duychien.fixmate.feature.tasklist

import com.duychien.fixmate.core.common.AppError
import com.duychien.fixmate.domain.model.Task

data class TaskListUiState(
    val tasks: List<Task> = emptyList(),
    val query: String = "",
    val filter: TaskFilter = TaskFilter.ALL,
    /** True only while the very first load runs against an empty database. */
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val error: AppError? = null,
) {
    val isEmpty: Boolean get() = tasks.isEmpty() && !isLoading
    val isFiltering: Boolean get() = query.isNotBlank() || filter != TaskFilter.ALL
}
