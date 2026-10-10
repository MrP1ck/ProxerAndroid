package me.proxer.app.ui.components

import android.content.Context
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE

/**
 * The [SnackbarHostState] of the enclosing scaffold. Screens show their snackbars through it instead of creating their
 * own host, so the snackbars are placed correctly above the navigation bar and floating action buttons.
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

/**
 * Shows the [error] as a snackbar with its button, which calls [onAction] when clicked. [message] defaults to the
 * message of the error.
 */
suspend fun SnackbarHostState.showErrorSnackbar(
    context: Context,
    error: ErrorAction,
    onAction: (ErrorAction) -> Unit,
    message: String = context.getString(error.message)
) {
    val actionLabel = when (error.buttonMessage) {
        ACTION_MESSAGE_HIDE -> null
        ACTION_MESSAGE_DEFAULT -> context.getString(R.string.error_action_retry)
        else -> context.getString(error.buttonMessage)
    }

    val result = showSnackbar(message = message, actionLabel = actionLabel, duration = SnackbarDuration.Long)

    if (result == SnackbarResult.ActionPerformed) {
        onAction(error)
    }
}
