package com.duychien.fixmate.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.duychien.fixmate.R
import com.duychien.fixmate.core.common.AppError

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    AppError.Server -> R.string.error_server
    AppError.Unknown -> R.string.error_unknown
}

@Composable
fun AppError.toMessage(): String = stringResource(messageRes())
