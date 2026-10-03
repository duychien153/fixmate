package com.duychien.fixmate.domain.usecase

import com.duychien.fixmate.domain.model.Task
import com.duychien.fixmate.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(title: String, completed: Boolean = false): Task =
        repository.createTask(title.trim(), completed)
}
