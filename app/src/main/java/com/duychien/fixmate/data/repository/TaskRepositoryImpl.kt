package com.duychien.fixmate.data.repository

import com.duychien.fixmate.core.common.Clock
import com.duychien.fixmate.core.common.Constants
import com.duychien.fixmate.core.common.DispatcherProvider
import com.duychien.fixmate.core.database.dao.TaskDao
import com.duychien.fixmate.core.database.entity.TaskEntity
import com.duychien.fixmate.core.network.FixMateApi
import com.duychien.fixmate.core.network.dto.TaskDto
import com.duychien.fixmate.data.mapper.toCreateRequest
import com.duychien.fixmate.data.mapper.toDomain
import com.duychien.fixmate.data.mapper.toEntity
import com.duychien.fixmate.data.mapper.toUpdateRequest
import com.duychien.fixmate.data.worker.SyncScheduler
import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.repository.SettingsRepository
import com.duychien.fixmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * Offline-first repository. Room is the single source of truth: the UI only ever
 * observes the database, every write is applied locally first (flagged with a
 * [SyncState]) and then pushed to the API. If the push fails the change stays
 * pending and WorkManager retries it once the device is back online.
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val api: FixMateApi,
    private val syncScheduler: SyncScheduler,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: Clock,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeTasks().map { entities -> entities.map(TaskEntity::toDomain) }

    override fun observeTask(id: Long): Flow<Task?> =
        taskDao.observeTask(id).map { it?.toDomain() }

    override fun observePendingSyncCount(): Flow<Int> = taskDao.observePendingSyncCount()

    override suspend fun refreshTasks() = withContext(dispatchers.io) {
        val remoteTasks = fetchRemoteTasks()
        val local = taskDao.getAll().associateBy { it.id }
        val now = clock.now()

        val entities = remoteTasks.mapNotNull { dto ->
            val current = local[dto.id]
            when {
                // Never let a server snapshot clobber a change the user made offline.
                current != null && current.syncState != SyncState.SYNCED.name -> null
                // Unchanged rows keep their timestamp so a refresh doesn't reshuffle the list.
                current != null && current.hasSameContentAs(dto) -> null
                else -> dto.toEntity(fetchedAt = now)
            }
        }

        taskDao.upsertAll(entities)
        settingsRepository.setLastSyncAt(now)
    }

    override suspend fun createTask(title: String, completed: Boolean): Task = withContext(dispatchers.io) {
        val now = clock.now()
        val entity = TaskEntity(
            id = generateLocalId(now),
            title = title,
            completed = completed,
            userId = Constants.DEFAULT_USER_ID,
            updatedAt = now,
            syncState = SyncState.PENDING_CREATE.name,
        )
        taskDao.upsert(entity)
        pushOrSchedule(entity)
        (taskDao.getTask(entity.id) ?: entity).toDomain()
    }

    override suspend fun updateTask(task: Task) = withContext(dispatchers.io) {
        val existing = taskDao.getTask(task.id) ?: return@withContext
        // A task the server has never seen must stay PENDING_CREATE, otherwise
        // we'd PATCH an id that doesn't exist remotely.
        val nextState = when (SyncState.valueOf(existing.syncState)) {
            SyncState.PENDING_CREATE -> SyncState.PENDING_CREATE
            else -> SyncState.PENDING_UPDATE
        }
        val entity = existing.copy(
            title = task.title,
            completed = task.completed,
            updatedAt = clock.now(),
            syncState = nextState.name,
        )
        taskDao.upsert(entity)
        pushOrSchedule(entity)
    }

    override suspend fun toggleTask(id: Long) {
        val existing = withContext(dispatchers.io) { taskDao.getTask(id) } ?: return
        updateTask(existing.toDomain().copy(completed = !existing.completed))
    }

    override suspend fun deleteTask(id: Long) = withContext(dispatchers.io) {
        val existing = taskDao.getTask(id) ?: return@withContext
        if (existing.syncState == SyncState.PENDING_CREATE.name) {
            // Never reached the server, so there is nothing remote to delete.
            taskDao.delete(id)
            return@withContext
        }
        val entity = existing.copy(
            updatedAt = clock.now(),
            syncState = SyncState.PENDING_DELETE.name,
        )
        taskDao.upsert(entity)
        pushOrSchedule(entity)
    }

    override suspend fun syncPendingTasks() = withContext(dispatchers.io) {
        val pending = taskDao.getPendingSync()
        if (pending.isEmpty()) return@withContext
        pending.forEach { push(it) }
        settingsRepository.setLastSyncAt(clock.now())
    }

    private fun TaskEntity.hasSameContentAs(dto: TaskDto): Boolean =
        title == dto.todo && completed == dto.completed && userId == dto.userId

    private suspend fun fetchRemoteTasks(): List<TaskDto> {
        val collected = mutableListOf<TaskDto>()
        var skip = 0
        var total: Int
        do {
            val page = api.getTasks(limit = Constants.PAGE_SIZE, skip = skip)
            collected += page.todos
            total = page.total
            skip += page.todos.size
        } while (page.todos.isNotEmpty() && skip < total && skip < Constants.MAX_REMOTE_TASKS)
        return collected
    }

    /**
     * Tries to push immediately for snappy UX; on any failure the change simply
     * stays pending and WorkManager picks it up when the network is back.
     */
    private suspend fun pushOrSchedule(entity: TaskEntity) {
        try {
            push(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            syncScheduler.requestSync()
        }
    }

    private suspend fun push(entity: TaskEntity) {
        when (SyncState.valueOf(entity.syncState)) {
            SyncState.SYNCED -> Unit

            SyncState.PENDING_CREATE -> {
                api.createTask(entity.toCreateRequest())
                markSynced(entity)
            }

            SyncState.PENDING_UPDATE -> {
                try {
                    api.updateTask(entity.id, entity.toUpdateRequest())
                } catch (e: HttpException) {
                    // The server lost (or never had) this task: re-create it instead of giving up.
                    if (e.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                        api.createTask(entity.toCreateRequest())
                    } else {
                        throw e
                    }
                }
                markSynced(entity)
            }

            SyncState.PENDING_DELETE -> {
                try {
                    api.deleteTask(entity.id)
                } catch (e: HttpException) {
                    if (e.code() != HttpURLConnection.HTTP_NOT_FOUND) throw e
                }
                taskDao.delete(entity.id)
            }
        }
    }

    /**
     * Only flips the row to SYNCED if it hasn't been edited again while the
     * network call was in flight; otherwise the newer edit stays pending.
     */
    private suspend fun markSynced(pushed: TaskEntity) {
        val current = taskDao.getTask(pushed.id) ?: return
        if (current.updatedAt == pushed.updatedAt && current.syncState == pushed.syncState) {
            taskDao.upsert(current.copy(syncState = SyncState.SYNCED.name))
        }
    }

    /**
     * DummyJSON never persists writes and always answers `POST /todos/add` with
     * the same id, so locally created tasks keep a client-generated id. A
     * millisecond timestamp can't collide with the server's small sequential ids.
     */
    private suspend fun generateLocalId(seed: Long): Long {
        var candidate = seed
        while (taskDao.getTask(candidate) != null) candidate++
        return candidate
    }
}
