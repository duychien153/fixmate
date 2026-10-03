package com.duychien.fixmate.data.repository

import app.cash.turbine.test
import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.testutil.FakeClock
import com.duychien.fixmate.testutil.FakeFixMateApi
import com.duychien.fixmate.testutil.FakeSettingsRepository
import com.duychien.fixmate.testutil.FakeSyncScheduler
import com.duychien.fixmate.testutil.FakeTaskDao
import com.duychien.fixmate.testutil.TestDispatcherProvider
import com.duychien.fixmate.testutil.dto
import com.duychien.fixmate.testutil.httpException
import com.duychien.fixmate.testutil.task
import com.duychien.fixmate.data.mapper.toEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TaskRepositoryImplTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeTaskDao
    private lateinit var api: FakeFixMateApi
    private lateinit var scheduler: FakeSyncScheduler
    private lateinit var settings: FakeSettingsRepository
    private lateinit var clock: FakeClock
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        dao = FakeTaskDao()
        api = FakeFixMateApi()
        scheduler = FakeSyncScheduler()
        settings = FakeSettingsRepository()
        clock = FakeClock()
        repository = TaskRepositoryImpl(
            taskDao = dao,
            api = api,
            syncScheduler = scheduler,
            settingsRepository = settings,
            dispatchers = TestDispatcherProvider(dispatcher),
            clock = clock,
        )
    }

    @Test
    fun refreshTasks_savesRemoteTasksToDatabase() = runTest(dispatcher) {
        api.remoteTasks = listOf(dto(1, "Replace air filter"), dto(2, "Check battery", completed = true))

        repository.refreshTasks()

        val tasks = repository.observeTasks().first()
        assertEquals(2, tasks.size)
        assertTrue(tasks.all { it.syncState == SyncState.SYNCED })
        assertEquals("Check battery", tasks.first { it.id == 2L }.title)
        assertEquals(clock.now(), settings.lastSyncAt.first())
    }

    @Test
    fun refreshTasks_pagesThroughRemoteResults() = runTest(dispatcher) {
        api.remoteTasks = (1L..45L).map { dto(it) }

        repository.refreshTasks()

        assertEquals(45, repository.observeTasks().first().size)
    }

    @Test
    fun refreshTasks_doesNotOverwriteLocallyPendingTask() = runTest(dispatcher) {
        dao.upsert(task(1, title = "Edited offline", syncState = SyncState.PENDING_UPDATE).toEntity())
        api.remoteTasks = listOf(dto(1, "Server title"), dto(2))

        repository.refreshTasks()

        val local = repository.observeTask(1).first()
        assertEquals("Edited offline", local?.title)
        assertEquals(SyncState.PENDING_UPDATE, local?.syncState)
        assertNotNull(repository.observeTask(2).first())
    }

    @Test
    fun refreshTasks_keepsTimestampOfUnchangedRowsAndBumpsChangedOnes() = runTest(dispatcher) {
        dao.upsertAll(
            listOf(
                task(1, title = "Same", updatedAt = 10).toEntity(),
                task(2, title = "Old title", updatedAt = 20).toEntity(),
            ),
        )
        api.remoteTasks = listOf(dto(1, "Same").copy(userId = 1), dto(2, "New title").copy(userId = 1))
        clock.current = 500

        repository.refreshTasks()

        assertEquals(10L, repository.observeTask(1).first()?.updatedAt)
        assertEquals(500L, repository.observeTask(2).first()?.updatedAt)
        assertEquals("New title", repository.observeTask(2).first()?.title)
    }

    @Test
    fun createTask_offline_marksPendingCreateAndSchedulesSync() = runTest(dispatcher) {
        api.failure = IOException("no network")

        val created = repository.createTask("Fix leaking tap")

        assertEquals(SyncState.PENDING_CREATE, created.syncState)
        assertEquals(1, scheduler.requestCount)
        assertEquals(1, repository.observePendingSyncCount().first())
    }

    @Test
    fun createTask_online_marksSyncedImmediately() = runTest(dispatcher) {
        val created = repository.createTask("Fix leaking tap", completed = false)

        assertEquals(SyncState.SYNCED, created.syncState)
        assertEquals(0, scheduler.requestCount)
        assertEquals("Fix leaking tap", api.createdRequests.single().todo)
    }

    @Test
    fun syncPendingTask_success_marksSynced() = runTest(dispatcher) {
        api.failure = IOException("offline")
        repository.createTask("Offline task")
        api.failure = null

        repository.syncPendingTasks()

        val tasks = repository.observeTasks().first()
        assertEquals(SyncState.SYNCED, tasks.single().syncState)
        assertEquals(0, repository.observePendingSyncCount().first())
    }

    @Test
    fun syncPendingTask_networkFailure_keepsPendingAndThrows() = runTest(dispatcher) {
        api.failure = IOException("offline")
        repository.createTask("Offline task")

        var thrown: Throwable? = null
        try {
            repository.syncPendingTasks()
        } catch (e: IOException) {
            thrown = e
        }

        assertNotNull(thrown)
        assertEquals(SyncState.PENDING_CREATE, repository.observeTasks().first().single().syncState)
    }

    @Test
    fun updateTask_onSyncedTask_marksPendingUpdateThenSyncs() = runTest(dispatcher) {
        dao.upsert(task(7, title = "Old").toEntity())

        repository.updateTask(task(7, title = "New", completed = true))

        val updated = repository.observeTask(7).first()
        assertEquals("New", updated?.title)
        assertTrue(updated?.completed == true)
        assertEquals(SyncState.SYNCED, updated?.syncState)
        assertEquals(7L, api.updatedRequests.single().first)
    }

    @Test
    fun updateTask_whenServerReturns404_recreatesTaskRemotely() = runTest(dispatcher) {
        dao.upsert(task(99).toEntity())
        api.updateFailure = httpException(404)

        repository.updateTask(task(99, title = "Renamed"))

        assertEquals("Renamed", api.createdRequests.single().todo)
        assertEquals(0, scheduler.requestCount)
        assertEquals(SyncState.SYNCED, repository.observeTask(99).first()?.syncState)
    }

    @Test
    fun deleteTask_whenServerReturns404_treatsAsDeleted() = runTest(dispatcher) {
        dao.upsert(task(42).toEntity())
        api.deleteFailure = httpException(404)

        repository.deleteTask(42)

        assertNull(dao.snapshot[42L])
        assertEquals(0, scheduler.requestCount)
    }

    @Test
    fun deleteTask_pendingCreate_removesLocallyWithoutRemoteCall() = runTest(dispatcher) {
        api.failure = IOException("offline")
        val created = repository.createTask("Never synced")

        repository.deleteTask(created.id)

        assertNull(repository.observeTask(created.id).first())
        assertTrue(api.deletedIds.isEmpty())
    }

    @Test
    fun deleteTask_syncedTask_hidesItUntilRemoteDeleteSucceeds() = runTest(dispatcher) {
        dao.upsert(task(3).toEntity())
        api.failure = IOException("offline")

        repository.deleteTask(3)

        repository.observeTasks().test {
            assertFalse(awaitItem().any { it.id == 3L })
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(SyncState.PENDING_DELETE.name, dao.snapshot[3L]?.syncState)

        api.failure = null
        repository.syncPendingTasks()

        assertNull(dao.snapshot[3L])
        assertEquals(listOf(3L), api.deletedIds)
    }

    @Test
    fun toggleTask_flipsCompletedFlag() = runTest(dispatcher) {
        dao.upsert(task(5, completed = false).toEntity())

        repository.toggleTask(5)

        assertTrue(repository.observeTask(5).first()?.completed == true)
    }
}
