package com.bernaferrari.quietguard.ui.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn

/** Simple async list/data state for screen ViewModels backed by cold Flows. */
data class UiAsyncState<T>(
    val data: T,
    val isLoading: Boolean = false,
    val hasReceived: Boolean = false,
    val error: Throwable? = null,
) {
    val isReady: Boolean get() = hasReceived && !isLoading
    val isInitialLoading: Boolean get() = isLoading && !hasReceived
    val isRefreshing: Boolean get() = isLoading && hasReceived
    val hasFailed: Boolean get() = error != null
}

fun <T> Flow<T>.asUiAsyncState(
    scope: CoroutineScope,
    initialData: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(5_000),
    retryRequests: Flow<Unit>? = null,
): StateFlow<UiAsyncState<T>> {
    var latestData = initialData
    var hasReceived = false

    return this
        .map { value ->
            latestData = value
            hasReceived = true
            UiAsyncState(data = value, isLoading = false, hasReceived = true)
        }
        .onStart {
            emit(UiAsyncState(data = latestData, isLoading = true, hasReceived = hasReceived))
        }
        .retryWhen { cause, _ ->
            emit(
                UiAsyncState(
                    data = latestData,
                    isLoading = false,
                    hasReceived = hasReceived,
                    error = cause,
                ),
            )
            retryRequests?.awaitRetry() ?: false
        }
        .stateIn(
            scope = scope,
            started = started,
            initialValue = UiAsyncState(data = initialData, isLoading = true, hasReceived = false),
        )
}

private suspend fun Flow<Unit>.awaitRetry(): Boolean {
    first()
    return true
}
