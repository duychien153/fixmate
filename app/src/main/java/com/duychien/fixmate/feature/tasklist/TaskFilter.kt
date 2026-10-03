package com.duychien.fixmate.feature.tasklist

import androidx.annotation.StringRes
import com.duychien.fixmate.R
import com.duychien.fixmate.domain.model.Task

enum class TaskFilter(@StringRes val labelRes: Int) {
    ALL(R.string.filter_all),
    PENDING(R.string.filter_pending),
    COMPLETED(R.string.filter_completed),
    ;

    fun matches(task: Task): Boolean = when (this) {
        ALL -> true
        PENDING -> !task.completed
        COMPLETED -> task.completed
    }
}
