package me.proxer.app.util.extension

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.proxer.app.exception.PartialException
import me.proxer.library.ProxerCall
import me.proxer.library.ProxerException
import me.proxer.library.api.Endpoint
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Executes this call without blocking and cancels it together with the calling coroutine.
 */
suspend fun <T> ProxerCall<T>.awaitNullable(): T? = suspendCancellableCoroutine { continuation ->
    val call = clone()

    continuation.invokeOnCancellation { call.cancel() }

    call.enqueue(
        { result -> if (continuation.isActive) continuation.resume(result) },
        { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    )
}

suspend fun <T : Any> ProxerCall<T>.await(): T = awaitNullable() ?: throw ProxerException(
    ProxerException.ErrorType.UNKNOWN,
    null,
    "Response is null.",
    NullPointerException("Response is null.")
)

suspend fun <T : Any> Endpoint<T>.await(): T = build().await()

@JvmName("awaitNullableEndpoint")
suspend fun <T : Any> Endpoint<T?>.awaitNullable(): T? = build().awaitNullable()

/**
 * Like [await], but wraps errors in a [PartialException] holding the [input] (e.g. an optimistic update).
 */
suspend fun <I, T : Any> Endpoint<T>.awaitPartial(input: I): T = try {
    await()
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    throw PartialException(error, input)
}

/**
 * Executes this call without blocking. The caller has to close the response.
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    val call = clone()

    continuation.invokeOnCancellation { call.cancel() }

    call.enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    })
}

/**
 * Executes this call without blocking and returns the body as a string.
 */
suspend fun Call.awaitString(): String = await().use { response ->
    // Reading the body blocks until it is complete.
    withContext(Dispatchers.IO) {
        response.body?.string() ?: throw IOException(NullPointerException("body is null"))
    }
}

/**
 * Runs [block] and retries it up to [maxRetries] times in total, waiting [delayMillis] between the attempts.
 */
suspend fun <T> retryWithDelay(maxRetries: Int, delayMillis: Long, block: suspend () -> T): T {
    var attempts = 0

    while (true) {
        try {
            return block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            if (++attempts >= maxRetries) throw error

            delay(delayMillis)
        }
    }
}
