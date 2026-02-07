package com.zeynbakers.order_management_system.core.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf

sealed interface UiEvent {
    data class Snackbar(
        val message: String,
        val actionLabel: String? = null,
        val withDismissAction: Boolean = false,
        val duration: SnackbarDuration = SnackbarDuration.Short
    ) : UiEvent
}

fun interface UiEventDispatcher {
    suspend fun dispatch(event: UiEvent): SnackbarResult?
}

val LocalUiEventDispatcher = staticCompositionLocalOf<UiEventDispatcher> {
    UiEventDispatcher { null }
}

suspend fun UiEventDispatcher.showSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult? {
    return dispatch(
        UiEvent.Snackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = duration
        )
    )
}
