package com.duychien.fixmate.domain.model

data class Task(
    val id: Long,
    val title: String,
    val completed: Boolean,
    val userId: Long,
    val updatedAt: Long,
    val syncState: SyncState,
) {
    val isSynced: Boolean get() = syncState == SyncState.SYNCED
}

/**
 * Tracks what still has to be pushed to the server. Room is the source of truth,
 * so every local write lands in the database first and is marked pending until
 * the remote call succeeds.
 */
enum class SyncState {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
}
