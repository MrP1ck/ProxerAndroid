package me.proxer.app.anime.stream

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.MainApplication.Companion.USER_AGENT
import timber.log.Timber

/**
 * Loads frames of a video for the preview while seeking.
 *
 * @author Ruben Gees
 */
class PreviewLoader(private val uri: Uri, private val referer: String?, private val isProxerStream: Boolean) {

    private val retriever = MediaMetadataRetriever()
    private val mutex = Mutex()

    private var isInitialized = false
    private var isFailed = false

    /**
     * Returns the frame at [positionMs], scaled to fit [width] and [height] where supported, or null if the frame
     * could not be loaded.
     */
    suspend fun frameAt(positionMs: Long, width: Int, height: Int): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!initialize()) return@withLock null

            try {
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 ->
                        retriever.getScaledFrameAtTime(
                            positionMs * 1000,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            width,
                            height
                        )
                    else -> retriever.getFrameAtTime(positionMs * 1000)
                }
            } catch (error: RuntimeException) {
                Timber.w(error)

                null
            }
        }
    }

    /**
     * Opens the video ahead of time, which can take a while for remote videos.
     */
    suspend fun prepare() = withContext(Dispatchers.IO) {
        mutex.withLock { initialize() }
    }

    fun release() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.withLock { retriever.release() }
        }
    }

    private fun initialize(): Boolean {
        if (!isInitialized && !isFailed) {
            try {
                retriever.setDataSource(uri.toString(), headers())

                isInitialized = true
            } catch (error: RuntimeException) {
                // MediaMetadataRetriever throws on some devices due to bugs in the implementation.
                Timber.w(error)

                isFailed = true
            }
        }

        return isInitialized
    }

    private fun headers() = buildMap {
        if (referer != null) put("Referer", referer)

        put("User-Agent", if (isProxerStream) USER_AGENT else GENERIC_USER_AGENT)
    }
}
