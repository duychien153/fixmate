package com.duychien.fixmate.testutil

import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Repository fake for ViewModel tests; behaviour is driven by plain in-memory state. */
class FakeTaskRepository(initial: List<Task> = emptyList()) : TaskRepository {

    private val tasks = MutableStateFlow(initial)

    /** When set, [refreshTasks] throws this instead of succeeding. */
    var refreshFailure: Throwable? = null
    var refreshCount = 0
        private set

    val current: List<Task> get() = tasks.value

    override fun observeTasks(): Flow<List<Task>> = tasks

    override fun observeTask(id: Long): Flow<Task?> = tasks.map { list -> list.firstOrNull { it.id == id } }

    override fun observePendingSyncCount(): Flow<Int> = tasks.map { list -> list.count { !it.isSynced } }

    override suspend fun refreshTasks() {
        refreshCount++
        refreshFailure?.let { throw it }
    }

    override suspend fun createTask(title: String, completed: Boolean): Task {
        val created = task(
            id = (tasks.value.maxOfOrNull { it.id } ?: 0L) + 1,
            title = title,
            completed = completed,
            syncState = SyncState.PENDING_CREATE,
        )
        tasks.update { it + created }
        return created
    }

    override suspend fun updateTask(task: Task) {
        tasks.update { list -> list.map { if (it.id == task.id) task.copy(syncState = SyncState.PENDING_UPDATE) else it } }
    }

    override suspend fun toggleTask(id: Long) {
        tasks.update { list -> list.map { if (it.id == id) it.copy(completed = !it.completed) else it } }
    }

    override suspend fun deleteTask(id: Long) {
        tasks.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun syncPendingTasks() {
        tasks.update { list -> list.map { it.copy(syncState = SyncState.SYNCED) } }
    }
}
