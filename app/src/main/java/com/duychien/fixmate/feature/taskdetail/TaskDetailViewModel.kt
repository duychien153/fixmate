package com.duychien.fixmate.feature.taskdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duychien.fixmate.core.common.AppResult
import com.duychien.fixmate.core.common.safeCall
import com.duychien.fixmate.domain.usecase.DeleteTaskUseCase
import com.duychien.fixmate.domain.usecase.GetTaskUseCase
import com.duychien.fixmate.domain.usecase.ToggleTaskUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = TaskDetailViewModel.Factory::class)
class TaskDetailViewModel @AssistedInject constructor(
    @Assisted private val taskId: Long,
    getTask: GetTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(taskId: Long): TaskDetailViewModel
    }

    private val isProcessing = MutableStateFlow(false)
    private val deleted = MutableStateFlow(false)

    private val _events = Channel<TaskDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val uiState: StateFlow<TaskDetailUiState> = combine(
        getTask(taskId),
        isProcessing,
        deleted,
    ) { task, processing, wasDeleted ->
        TaskDetailUiState(
            task = task,
            // Once the user deletes, the row disappears from Room; don't flash "not found".
            isLoading = wasDeleted,
            isProcessing = processing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskDetailUiState(isLoading = true),
    )

    fun onToggleCompleted() {
        runAction { toggleTask(taskId) }
    }

    fun onDelete() {
        deleted.value = true
        runAction(onSuccess = { _events.send(TaskDetailEvent.Deleted) }) { deleteTask(taskId) }
    }

    private fun runAction(
        onSuccess: suspend () -> Unit = {},
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            isProcessing.value = true
            when (val result = safeCall { action() }) {
                is AppResult.Success -> onSuccess()
                is AppResult.Error -> {
                    deleted.value = false
                    _events.send(TaskDetailEvent.Error(result.error))
                }
            }
            isProcessing.value = false
        }
    }
}
