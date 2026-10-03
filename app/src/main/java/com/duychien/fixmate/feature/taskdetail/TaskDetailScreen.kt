package com.duychien.fixmate.feature.taskdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duychien.fixmate.R
import com.duychien.fixmate.core.ui.components.EmptyState
import com.duychien.fixmate.core.ui.components.LoadingState
import com.duychien.fixmate.core.ui.components.SyncStateBadge
import com.duychien.fixmate.core.ui.components.formatDateTime
import com.duychien.fixmate.core.ui.components.messageRes
import com.duychien.fixmate.domain.model.Task

@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel<TaskDetailViewModel, TaskDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(taskId) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                TaskDetailEvent.Deleted -> onBack()
                is TaskDetailEvent.Error -> snackbarHostState.showSnackbar(
                    context.getString(event.error.messageRes()),
                )
            }
        }
    }

    TaskDetailContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onEdit = { onEdit(taskId) },
        onToggleCompleted = viewModel::onToggleCompleted,
        onDelete = viewModel::onDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailContent(
    state: TaskDetailUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.task_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when {
                state.isLoading -> LoadingState()
                state.isNotFound -> EmptyState(
                    icon = Icons.Default.Warning,
                    title = stringResource(R.string.task_detail_not_found),
                    message = "",
                    actionLabel = stringResource(R.string.action_back),
                    onAction = onBack,
                )
                else -> TaskDetailBody(
                    task = state.task!!,
                    enabled = !state.isProcessing,
                    onEdit = onEdit,
                    onToggleCompleted = onToggleCompleted,
                    onDeleteClick = { showDeleteDialog = true },
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.task_detail_delete_title)) },
            text = { Text(stringResource(R.string.task_detail_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                ) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun TaskDetailBody(
    task: Task,
    enabled: Boolean,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow(
                    label = stringResource(R.string.task_detail_label_status),
                    value = stringResource(
                        if (task.completed) R.string.status_completed else R.string.status_pending,
                    ),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                DetailRow(label = stringResource(R.string.task_detail_label_id), value = task.id.toString())
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                DetailRow(label = stringResource(R.string.task_detail_label_user), value = task.userId.toString())
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                DetailRow(
                    label = stringResource(R.string.task_detail_label_updated),
                    value = formatDateTime(task.updatedAt),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.task_detail_label_sync),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SyncStateBadge(syncState = task.syncState)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = onToggleCompleted,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (task.completed) R.string.task_detail_mark_pending else R.string.task_detail_mark_completed,
                ),
            )
        }
        OutlinedButton(
            onClick = onEdit,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_edit))
        }
        OutlinedButton(
            onClick = onDeleteClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
