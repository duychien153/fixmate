package com.duychien.fixmate.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.duychien.fixmate.core.database.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    /** Tasks awaiting remote deletion stay in the table until synced but are hidden from the UI. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE syncState != 'PENDING_DELETE'
        ORDER BY updatedAt DESC, id ASC
        """,
    )
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM tasks WHERE syncState != 'SYNCED' ORDER BY updatedAt ASC")
    suspend fun getPendingSync(): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM tasks WHERE syncState != 'SYNCED'")
    fun observePendingSyncCount(): Flow<Int>
}
