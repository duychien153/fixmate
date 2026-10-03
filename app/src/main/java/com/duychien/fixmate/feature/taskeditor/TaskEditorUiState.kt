package com.duychien.fixmate.feature.taskeditor

import com.duychien.fixmate.core.common.AppError

data class TaskEditorUiState(
    val title: String = "",
    val completed: Boolean = false,
    val titleError: TitleError? = null,
    val isEditMode: Boolean = false,
    /** True while an existing task is being loaded into the form. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
) {
    val canSave: Boolean get() = !isLoading && !isSaving
}

enum class TitleError {
    BLANK,
    TOO_SHORT,
}

sealed interface TaskEditorEvent {
    data object Saved : TaskEditorEvent
    data class Error(val error: AppError) : TaskEditorEvent
}
