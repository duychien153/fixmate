package com.duychien.fixmate.testutil

import com.duychien.fixmate.core.network.FixMateApi
import com.duychien.fixmate.core.network.dto.CreateTaskRequest
import com.duychien.fixmate.core.network.dto.TaskDto
import com.duychien.fixmate.core.network.dto.TaskListResponse
import com.duychien.fixmate.core.network.dto.UpdateTaskRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Scriptable fake of the DummyJSON API. Set [failure] to make every call throw. */
class FakeFixMateApi : FixMateApi {

    var remoteTasks: List<TaskDto> = emptyList()
    var failure: Throwable? = null

    /** Endpoint-specific failures, checked after the global [failure]. */
    var updateFailure: Throwable? = null
    var deleteFailure: Throwable? = null

    val createdRequests = mutableListOf<CreateTaskRequest>()
    val updatedRequests = mutableListOf<Pair<Long, UpdateTaskRequest>>()
    val deletedIds = mutableListOf<Long>()

    override suspend fun getTasks(limit: Int, skip: Int): TaskListResponse {
        failIfConfigured()
        val page = remoteTasks.drop(skip).take(limit)
        return TaskListResponse(todos = page, total = remoteTasks.size, skip = skip, limit = limit)
    }

    override suspend fun getTask(id: Long): TaskDto {
        failIfConfigured()
        return remoteTasks.first { it.id == id }
    }

    override suspend fun createTask(request: CreateTaskRequest): TaskDto {
        failIfConfigured()
        createdRequests += request
        // DummyJSON always answers with the same fake id for simulated adds.
        return TaskDto(id = 255, todo = request.todo, completed = request.completed, userId = request.userId)
    }

    override suspend fun updateTask(id: Long, request: UpdateTaskRequest): TaskDto {
        failIfConfigured()
        updateFailure?.let { throw it }
        updatedRequests += id to request
        return TaskDto(id = id, todo = request.todo.orEmpty(), completed = request.completed ?: false, userId = 1)
    }

    override suspend fun deleteTask(id: Long): TaskDto {
        failIfConfigured()
        deleteFailure?.let { throw it }
        deletedIds += id
        return TaskDto(id = id, todo = "", completed = false, userId = 1)
    }

    private fun failIfConfigured() {
        failure?.let { throw it }
    }
}

fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, "{}".toResponseBody("application/json".toMediaType())))

fun dto(id: Long, todo: String = "Remote $id", completed: Boolean = false): TaskDto =
    TaskDto(id = id, todo = todo, completed = completed, userId = 26)
