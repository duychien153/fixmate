package com.duychien.fixmate.core.network.dto

data class CreateTaskRequest(
    val todo: String,
    val completed: Boolean,
    val userId: Long,
)

/** Fields are nullable so a PATCH only carries what actually changed (Gson omits nulls). */
data class UpdateTaskRequest(
    val todo: String? = null,
    val completed: Boolean? = null,
)
