package com.duychien.fixmate.feature.taskdetail

import com.duychien.fixmate.core.common.AppError
import com.duychien.fixmate.domain.model.Task

data class TaskDetailUiState(
    val task: Task? = null,
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
) {
    val isNotFound: Boolean get() = !isLoading && task == null
}

sealed interface TaskDetailEvent {
    data object Deleted : TaskDetailEvent
    data class Error(val error: AppError) : TaskDetailEvent
}
