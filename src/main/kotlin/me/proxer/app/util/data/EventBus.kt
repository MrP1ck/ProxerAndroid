package me.proxer.app.util.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

/**
 * Delivers events between unrelated parts of the app, e.g. from a worker to the visible screen.
 *
 * [post] reports whether somebody is currently collecting the type of the event. The workers use this to find out
 * if a screen is visible (and a notification would be redundant).
 */
class EventBus {

    private val flows = ConcurrentHashMap<Class<*>, MutableSharedFlow<Any>>()

    private fun flowFor(type: Class<*>) = flows.getOrPut(type) {
        MutableSharedFlow(extraBufferCapacity = BUFFER_SIZE, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    /**
     * Collecting the returned flow registers for events of the [type], until the collection ends.
     */
    fun <T : Any> events(type: Class<T>): Flow<T> = flowFor(type).map { type.cast(it) as T }

    inline fun <reified T : Any> events(): Flow<T> = events(T::class.java)

    /**
     * Returns true if at least one collector is registered for the type of the event.
     */
    fun post(event: Any): Boolean {
        val flow = flowFor(event.javaClass)
        val hasCollectors = flow.subscriptionCount.value > 0

        flow.tryEmit(event)

        return hasCollectors
    }

    private companion object {
        private const val BUFFER_SIZE = 16
    }
}
