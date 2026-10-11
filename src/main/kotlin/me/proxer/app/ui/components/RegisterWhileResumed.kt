package me.proxer.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch
import me.proxer.app.util.data.EventBus
import org.koin.compose.koinInject

/**
 * Registers for [eventClass] on the bus while the screen is resumed. The MessengerWorker posts these events to find
 * out whether a conversation or the conferences are visible, and synchronizes more often then.
 */
@Composable
fun <T : Any> RegisterWhileResumed(eventClass: Class<T>) {
    val bus = koinInject<EventBus>()
    val scope = rememberCoroutineScope()

    LifecycleResumeEffect(eventClass) {
        val job = scope.launch { bus.events(eventClass).collect { } }

        onPauseOrDispose { job.cancel() }
    }
}
