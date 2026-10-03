package com.duychien.fixmate.core.network.dto

data class TaskListResponse(
    val todos: List<TaskDto>,
    val total: Int,
    val skip: Int,
    val limit: Int,
)
