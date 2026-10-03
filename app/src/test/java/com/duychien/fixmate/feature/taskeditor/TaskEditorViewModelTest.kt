package com.duychien.fixmate.feature.taskeditor

import app.cash.turbine.test
import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.domain.usecase.CreateTaskUseCase
import com.duychien.fixmate.domain.usecase.GetTaskUseCase
import com.duychien.fixmate.domain.usecase.UpdateTaskUseCase
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

class TaskEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun createViewModel(repository: FakeTaskRepository, taskId: Long? = null) = TaskEditorViewModel(
        taskId = taskId,
        getTask = GetTaskUseCase(repository),
        createTask = CreateTaskUseCase(repository),
        updateTask = UpdateTaskUseCase(repository),
    )

    @Test
    fun save_withBlankTitle_showsBlankError() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.onTitleChange("   ")
        viewModel.save()

        assertEquals(TitleError.BLANK, viewModel.uiState.value.titleError)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun save_withShortTitle_showsTooShortError() = runTest {
        val viewModel = createViewModel(FakeTaskRepository())

        viewModel.onTitleChange("ab")
        viewModel.save()

        assertEquals(TitleError.TOO_SHORT, viewModel.uiState.value.titleError)
    }

    @Test
    fun typing_clearsValidationError() = runTest {
        val viewModel = createViewModel(FakeTaskRepository())
        viewModel.save()
        assertEquals(TitleError.BLANK, viewModel.uiState.value.titleError)

        viewModel.onTitleChange("F")

        assertNull(viewModel.uiState.value.titleError)
    }

    @Test
    fun save_withValidTitle_createsTaskAndEmitsSaved() = runTest {
        val repository = FakeTaskRepository()
        val viewModel = createViewModel(repository)

        viewModel.events.test {
            viewModel.onTitleChange("  Replace air filter  ")
            viewModel.onCompletedChange(true)
            viewModel.save()

            assertEquals(TaskEditorEvent.Saved, awaitItem())
        }
        val created = repository.current.single()
        assertEquals("Replace air filter", created.title)
        assertTrue(created.completed)
        assertEquals(SyncState.PENDING_CREATE, created.syncState)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun editMode_prefillsFormAndUpdatesExistingTask() = runTest {
        val repository = FakeTaskRepository(listOf(task(10, title = "Old title", completed = false)))
        val viewModel = createViewModel(repository, taskId = 10)

        val loaded = viewModel.uiState.value
        assertTrue(loaded.isEditMode)
        assertFalse(loaded.isLoading)
        assertEquals("Old title", loaded.title)

        viewModel.events.test {
            viewModel.onTitleChange("New title")
            viewModel.save()
            assertEquals(TaskEditorEvent.Saved, awaitItem())
        }
        val updated = repository.current.single()
        assertEquals(10L, updated.id)
        assertEquals("New title", updated.title)
        assertEquals(SyncState.PENDING_UPDATE, updated.syncState)
    }
}
