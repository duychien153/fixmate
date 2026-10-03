package com.duychien.fixmate.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [Index("syncState"), Index("updatedAt")],
)
data class TaskEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val completed: Boolean,
    val userId: Long,
    val updatedAt: Long,
    /** Stored as the [com.duychien.fixmate.domain.model.SyncState] enum name. */
    val syncState: String,
)
