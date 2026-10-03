package com.duychien.fixmate.domain.usecase

import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(id: Long): Flow<Task?> = repository.observeTask(id)
}
