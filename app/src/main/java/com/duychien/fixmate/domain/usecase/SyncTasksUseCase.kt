package com.duychien.fixmate.domain.usecase

import com.duychien.fixmate.domain.repository.TaskRepository
import javax.inject.Inject

class SyncTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke() = repository.syncPendingTasks()
}
