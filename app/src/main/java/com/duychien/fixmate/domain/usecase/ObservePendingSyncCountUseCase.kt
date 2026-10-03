package com.duychien.fixmate.domain.usecase

import com.duychien.fixmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePendingSyncCountUseCase @Inject constructor(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observePendingSyncCount()
}
