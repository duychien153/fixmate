package com.duychien.fixmate.data.mapper

import com.duychien.fixmate.core.database.entity.TaskEntity
import com.duychien.fixmate.core.network.dto.CreateTaskRequest
import com.duychien.fixmate.core.network.dto.TaskDto
import com.duychien.fixmate.core.network.dto.UpdateTaskRequest
import com.duychien.fixmate.domain.model.SyncState
import com.duychien.fixmate.domain.model.Task

fun TaskDto.toEntity(fetchedAt: Long): TaskEntity =
    TaskEntity(
        id = id,
        title = todo,
        completed = completed,
        userId = userId,
        updatedAt = fetchedAt,
        syncState = SyncState.SYNCED.name,
    )

fun TaskEntity.toDomain(): Task =
    Task(
        id = id,
        title = title,
        completed = completed,
        userId = userId,
        updatedAt = updatedAt,
        syncState = SyncState.valueOf(syncState),
    )

fun Task.toEntity(): TaskEntity =
    TaskEntity(
        id = id,
        title = title,
        completed = completed,
        userId = userId,
        updatedAt = updatedAt,
        syncState = syncState.name,
    )

fun TaskEntity.toCreateRequest(): CreateTaskRequest =
    CreateTaskRequest(todo = title, completed = completed, userId = userId)

fun TaskEntity.toUpdateRequest(): UpdateTaskRequest =
    UpdateTaskRequest(todo = title, completed = completed)
