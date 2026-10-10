package me.proxer.app.ui.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.rubengees.rxbus.RxBus
import org.koin.compose.koinInject

/**
 * Subscribes to [eventClass] on the bus while the screen is resumed. The MessengerWorker posts these events to find
 * out whether a conversation or the conferences are visible, and synchronizes more often then.
 */
@Composable
fun <T : Any> RegisterWhileResumed(eventClass: Class<T>) {
    val bus = koinInject<RxBus>()

    LifecycleResumeEffect(eventClass) {
        val disposable = bus.register(eventClass).subscribe()

        onPauseOrDispose { disposable.dispose() }
    }
}
