package me.proxer.app.util.data

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * One-shot events of a ViewModel, e.g. an error to show in a snackbar. Every event is delivered once, as soon as
 * somebody collects them. Observe them in Compose with [me.proxer.app.ui.components.ObserveAsEvents].
 */
class Events<T : Any> {

    private val channel = Channel<T>(Channel.UNLIMITED)

    val flow: Flow<T> = channel.receiveAsFlow()

    fun send(event: T) {
        channel.trySend(event)
    }
}

fun Events<Unit>.send() = send(Unit)
