package com.duychien.fixmate.core.network.dto

/** Shape of a single todo returned by https://dummyjson.com/todos */
data class TaskDto(
    val id: Long,
    val todo: String,
    val completed: Boolean,
    val userId: Long,
)
