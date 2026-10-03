package com.duychien.fixmate.domain.usecase

import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<Task>> = repository.observeTasks()
}
