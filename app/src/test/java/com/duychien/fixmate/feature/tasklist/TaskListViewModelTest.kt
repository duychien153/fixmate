package com.duychien.fixmate.feature.tasklist

import app.cash.turbine.test
import com.duychien.fixmate.core.common.AppError
import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.domain.usecase.GetTasksUseCase
import com.duychien.fixmate.domain.usecase.ObservePendingSyncCountUseCase
import com.duychien.fixmate.domain.usecase.RefreshTasksUseCase
import com.duychien.fixmate.domain.usecase.ToggleTaskUseCase
import com.duychien.fixmate.testutil.FakeTaskRepository
import com.duychien.fixmate.testutil.MainDispatcherRule
import com.duychien.fixmate.testutil.task
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class TaskListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val seed = listOf(
        task(1, title = "Replace air filter"),
        task(2, title = "Check battery", completed = true),
        task(3, title = "Inspect brake pads", syncState = SyncState.PENDING_CREATE),
    )

    private fun createViewModel(repository: FakeTaskRepository) = TaskListViewModel(
        getTasks = GetTasksUseCase(repository),
        observePendingSyncCount = ObservePendingSyncCountUseCase(repository),
        refreshTasks = RefreshTasksUseCase(repository),
        toggleTask = ToggleTaskUseCase(repository),
    )

    @Test
    fun initialLoad_exposesCachedTasksAndTriggersRefresh() = runTest {
        val repository = FakeTaskRepository(seed)
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(3, state.tasks.size)
            assertFalse(state.isLoading)
            assertEquals(1, state.pendingSyncCount)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repository.refreshCount)
    }

    @Test
    fun searchQuery_filtersTasks() = runTest {
        val viewModel = createViewModel(FakeTaskRepository(seed))

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onQueryChange("batt")

            val filtered = awaitItem()
            assertEquals(listOf("Check battery"), filtered.tasks.map { it.title })
            assertEquals("batt", filtered.query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun completedFilter_returnsCompletedTasks() = runTest {
        val viewModel = createViewModel(FakeTaskRepository(seed))

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onFilterChange(TaskFilter.COMPLETED)

            val completed = awaitItem()
            assertTrue(completed.tasks.all { it.completed })
            assertEquals(1, completed.tasks.size)

            viewModel.onFilterChange(TaskFilter.PENDING)
            val pending = awaitItem()
            assertTrue(pending.tasks.none { it.completed })
            assertEquals(2, pending.tasks.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun refreshFailure_keepsCachedDataAndExposesNetworkError() = runTest {
        val repository = FakeTaskRepository(seed).apply { refreshFailure = IOException("offline") }
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertEquals(3, state.tasks.size)
            assertEquals(AppError.Network, state.error)
            assertFalse(state.isRefreshing)

            viewModel.onErrorShown()
            assertNull(awaitItem().error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emptyDatabase_showsLoadingOnlyUntilFirstRefreshFinishes() = runTest {
        val repository = FakeTaskRepository(emptyList()).apply { refreshFailure = IOException("offline") }
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun onToggleTask_updatesRepository() = runTest {
        val repository = FakeTaskRepository(seed)
        val viewModel = createViewModel(repository)

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onToggleTask(1)

            val state = awaitItem()
            assertTrue(state.tasks.first { it.id == 1L }.completed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
