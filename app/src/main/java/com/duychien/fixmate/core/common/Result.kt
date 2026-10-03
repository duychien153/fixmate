package com.duychien.fixmate.core.common

import kotlin.coroutines.cancellation.CancellationException

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AppError, val cause: Throwable? = null) : AppResult<Nothing>
}

/**
 * Runs [block] and maps any failure to an [AppError]. Cancellation is always
 * rethrown so structured concurrency keeps working.
 */
suspend inline fun <T> safeCall(crossinline block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        AppResult.Error(t.toAppError(), t)
    }

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(error)
    return this
}
