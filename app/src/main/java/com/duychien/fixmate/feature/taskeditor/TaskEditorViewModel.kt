package com.duychien.fixmate.feature.taskeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duychien.fixmate.core.common.AppResult
import com.duychien.fixmate.core.common.Constants
import com.duychien.fixmate.core.common.safeCall
import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.usecase.CreateTaskUseCase
import com.duychien.fixmate.domain.usecase.GetTaskUseCase
import com.duychien.fixmate.domain.usecase.UpdateTaskUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TaskEditorViewModel.Factory::class)
class TaskEditorViewModel @AssistedInject constructor(
    @Assisted private val taskId: Long?,
    private val getTask: GetTaskUseCase,
    private val createTask: CreateTaskUseCase,
    private val updateTask: UpdateTaskUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(taskId: Long?): TaskEditorViewModel
    }

    private val _uiState = MutableStateFlow(
        TaskEditorUiState(isEditMode = taskId != null, isLoading = taskId != null),
    )
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<TaskEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Snapshot of the task being edited, so updates keep untouched fields intact. */
    private var original: Task? = null

    init {
        if (taskId != null) loadTask(taskId)
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onCompletedChange(value: Boolean) {
        _uiState.update { it.copy(completed = value) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return

        val error = validateTitle(state.title)
        if (error != null) {
            _uiState.update { it.copy(titleError = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val title = state.title.trim()
            val result = safeCall {
                val existing = original
                if (existing != null) {
                    updateTask(existing.copy(title = title, completed = state.completed))
                } else {
                    createTask(title, state.completed)
                }
            }
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is AppResult.Success -> _events.send(TaskEditorEvent.Saved)
                is AppResult.Error -> _events.send(TaskEditorEvent.Error(result.error))
            }
        }
    }

    private fun loadTask(id: Long) {
        viewModelScope.launch {
            val task = getTask(id).first()
            original = task
            _uiState.update {
                it.copy(
                    title = task?.title.orEmpty(),
                    completed = task?.completed ?: false,
                    isLoading = false,
                )
            }
        }
    }

    companion object {
        fun validateTitle(title: String): TitleError? {
            val trimmed = title.trim()
            return when {
                trimmed.isEmpty() -> TitleError.BLANK
                trimmed.length < Constants.MIN_TITLE_LENGTH -> TitleError.TOO_SHORT
                else -> null
            }
        }
    }
}
