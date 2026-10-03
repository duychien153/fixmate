package com.duychien.fixmate.domain.usecase

import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task) = repository.updateTask(task.copy(title = task.title.trim()))
}
