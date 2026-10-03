package com.duychien.fixmate.core.common

import retrofit2.HttpException
import java.io.IOException

/**
 * User-facing error categories. The UI only ever has to deal with these three
 * cases, so the mapping from exceptions lives in one place.
 */
sealed interface AppError {
    data object Network : AppError
    data object Server : AppError
    data object Unknown : AppError
}

fun Throwable.toAppError(): AppError = when (this) {
    is IOException -> AppError.Network
    is HttpException -> if (code() in 500..599) AppError.Server else AppError.Unknown
    else -> AppError.Unknown
}
