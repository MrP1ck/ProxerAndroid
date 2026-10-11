package me.proxer.app.util.extension

import android.content.BroadcastReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Runs [block] and returns its result. Errors are logged and returned as a failure, except for the cancellation of the
 * coroutine, which is always rethrown.
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> catchingErrors(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Timber.e(error)

    Result.failure(error)
}

/**
 * Runs [block] on a background thread and keeps the process alive until it is done. Errors are logged.
 */
fun BroadcastReceiver.launchAsync(block: suspend () -> Unit) {
    val pendingResult = goAsync()

    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            catchingErrors { block() }
        } finally {
            pendingResult.finish()
        }
    }
}
