package com.duychien.fixmate.navigation

import kotlinx.serialization.Serializable

sealed interface Destination {

    @Serializable
    data object TaskList : Destination

    @Serializable
    data class TaskDetail(val taskId: Long) : Destination

    /** `taskId == null` opens the editor in "create" mode. */
    @Serializable
    data class TaskEditor(val taskId: Long? = null) : Destination

    @Serializable
    data object Settings : Destination
}
