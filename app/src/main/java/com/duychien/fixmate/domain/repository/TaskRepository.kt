package com.duychien.fixmate.domain.repository

import com.duychien.fixmate.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {

    fun observeTasks(): Flow<List<Task>>

    fun observeTask(id: Long): Flow<Task?>

    fun observePendingSyncCount(): Flow<Int>

    /** Pulls tasks from the remote API into the local database. Throws on failure. */
    suspend fun refreshTasks()

    suspend fun createTask(title: String, completed: Boolean = false): Task

    suspend fun updateTask(task: Task)

    suspend fun toggleTask(id: Long)

    suspend fun deleteTask(id: Long)

    /** Pushes every locally pending change to the remote API. Throws on failure. */
    suspend fun syncPendingTasks()
}
