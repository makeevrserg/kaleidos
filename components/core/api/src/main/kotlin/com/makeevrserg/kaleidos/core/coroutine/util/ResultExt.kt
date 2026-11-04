package com.makeevrserg.kaleidos.core.coroutine.util

import kotlinx.coroutines.CancellationException

fun <T> Result<T>.propagateCancellationException(): Result<T> {
    return onFailure { throwable ->
        if (throwable !is CancellationException) return@onFailure
        throw throwable
    }
}
