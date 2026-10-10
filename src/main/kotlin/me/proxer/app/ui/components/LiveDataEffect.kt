package me.proxer.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

/**
 * Calls [onEvent] for every non-null value of the one-shot [liveData], e.g. a ResettingMutableLiveData, one after
 * another in a coroutine (so it can show snackbars).
 *
 * Unlike observeAsState, no value is lost if the LiveData resets itself right after notifying its observers: the
 * state would only see the reset, as both happen before the next frame.
 */
@Composable
fun <T> LiveDataEffect(liveData: LiveData<T>, onEvent: suspend CoroutineScope.(T & Any) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEvent by rememberUpdatedState(onEvent)
    val events = remember(liveData) { Channel<T & Any>(Channel.UNLIMITED) }

    DisposableEffect(liveData, lifecycleOwner) {
        val observer = Observer<T> { value -> if (value != null) events.trySend(value) }

        liveData.observe(lifecycleOwner, observer)

        onDispose { liveData.removeObserver(observer) }
    }

    LaunchedEffect(events) {
        for (event in events) currentOnEvent(event)
    }
}
