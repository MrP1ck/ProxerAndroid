package me.proxer.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proxer.app.util.data.Events

/**
 * Calls [onEvent] for every event, one after another in a coroutine (so it can show snackbars). Events are only
 * delivered while the screen is started and wait until it is otherwise, so none is lost.
 */
@Composable
fun <T : Any> ObserveAsEvents(events: Events<T>, onEvent: suspend CoroutineScope.(T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)

    LaunchedEffect(events, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) {
                events.flow.collect { event -> currentOnEvent(event) }
            }
        }
    }
}
