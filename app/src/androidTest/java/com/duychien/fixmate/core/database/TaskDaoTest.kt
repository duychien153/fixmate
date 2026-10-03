package com.duychien.fixmate.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duychien.fixmate.core.database.dao.TaskDao
import com.duychien.fixmate.core.database.entity.TaskEntity
import com.duychien.fixmate.domain.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var database: FixMateDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FixMateDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertAndObserveTask() = runTest {
        val entity = entity(id = 1, title = "Replace air filter")

        dao.upsert(entity)

        assertEquals(entity, dao.observeTask(1).first())
        assertEquals(listOf(entity), dao.observeTasks().first())
    }

    @Test
    fun upsert_updatesExistingRow() = runTest {
        dao.upsert(entity(id = 1, title = "Before"))

        dao.upsert(entity(id = 1, title = "After", completed = true))

        val stored = dao.observeTask(1).first()
        assertEquals("After", stored?.title)
        assertEquals(true, stored?.completed)
        assertEquals(1, dao.observeTasks().first().size)
    }

    @Test
    fun observeTasks_hidesPendingDeleteRowsAndOrdersByUpdatedAt() = runTest {
        dao.upsertAll(
            listOf(
                entity(id = 1, updatedAt = 100),
                entity(id = 2, updatedAt = 300),
                entity(id = 3, updatedAt = 200, syncState = SyncState.PENDING_DELETE),
            ),
        )

        val visible = dao.observeTasks().first()

        assertEquals(listOf(2L, 1L), visible.map { it.id })
    }

    @Test
    fun getPendingSync_returnsOnlyUnsyncedRows() = runTest {
        dao.upsertAll(
            listOf(
                entity(id = 1, syncState = SyncState.SYNCED),
                entity(id = 2, syncState = SyncState.PENDING_CREATE),
                entity(id = 3, syncState = SyncState.PENDING_UPDATE),
            ),
        )

        assertEquals(listOf(2L, 3L), dao.getPendingSync().map { it.id }.sorted())
        assertEquals(2, dao.observePendingSyncCount().first())
    }

    @Test
    fun delete_removesRow() = runTest {
        dao.upsert(entity(id = 1))

        dao.delete(1)

        assertNull(dao.observeTask(1).first())
    }

    private fun entity(
        id: Long,
        title: String = "Task $id",
        completed: Boolean = false,
        updatedAt: Long = id,
        syncState: SyncState = SyncState.SYNCED,
    ) = TaskEntity(
        id = id,
        title = title,
        completed = completed,
        userId = 1,
        updatedAt = updatedAt,
        syncState = syncState.name,
    )
}
