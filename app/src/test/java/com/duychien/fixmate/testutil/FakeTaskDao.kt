package com.duychien.fixmate.testutil

import com.duychien.fixmate.core.database.dao.TaskDao
import com.duychien.fixmate.core.database.entity.TaskEntity
import com.duychien.fixmate.domain.model.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory stand-in for the Room DAO that mirrors its query semantics. */
class FakeTaskDao : TaskDao {

    private val rows = MutableStateFlow<Map<Long, TaskEntity>>(emptyMap())

    val snapshot: Map<Long, TaskEntity> get() = rows.value

    override fun observeTasks(): Flow<List<TaskEntity>> = rows.map { map ->
        map.values
            .filter { it.syncState != SyncState.PENDING_DELETE.name }
            .sortedWith(compareByDescending<TaskEntity> { it.updatedAt }.thenBy { it.id })
    }

    override fun observeTask(id: Long): Flow<TaskEntity?> = rows.map { it[id] }

    override suspend fun getTask(id: Long): TaskEntity? = rows.value[id]

    override suspend fun getAll(): List<TaskEntity> = rows.value.values.toList()

    override suspend fun upsert(task: TaskEntity) {
        rows.update { it + (task.id to task) }
    }

    override suspend fun upsertAll(tasks: List<TaskEntity>) {
        rows.update { it + tasks.associateBy { task -> task.id } }
    }

    override suspend fun delete(id: Long) {
        rows.update { it - id }
    }

    override suspend fun getPendingSync(): List<TaskEntity> =
        rows.value.values.filter { it.syncState != SyncState.SYNCED.name }.sortedBy { it.updatedAt }

    override fun observePendingSyncCount(): Flow<Int> = rows.map { map ->
        map.values.count { it.syncState != SyncState.SYNCED.name }
    }
}
