package com.duychien.fixmate.core.network

import com.duychien.fixmate.core.common.Constants
import com.duychien.fixmate.core.network.dto.CreateTaskRequest
import com.duychien.fixmate.core.network.dto.TaskDto
import com.duychien.fixmate.core.network.dto.TaskListResponse
import com.duychien.fixmate.core.network.dto.UpdateTaskRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * DummyJSON todos API. Note that POST/PATCH/DELETE are *simulated* by the server:
 * they return a realistic response but nothing is persisted.
 */
interface FixMateApi {

    @GET("todos")
    suspend fun getTasks(
        @Query("limit") limit: Int = Constants.PAGE_SIZE,
        @Query("skip") skip: Int = 0,
    ): TaskListResponse

    @GET("todos/{id}")
    suspend fun getTask(@Path("id") id: Long): TaskDto

    @POST("todos/add")
    suspend fun createTask(@Body request: CreateTaskRequest): TaskDto

    @PATCH("todos/{id}")
    suspend fun updateTask(
        @Path("id") id: Long,
        @Body request: UpdateTaskRequest,
    ): TaskDto

    @DELETE("todos/{id}")
    suspend fun deleteTask(@Path("id") id: Long): TaskDto
}
