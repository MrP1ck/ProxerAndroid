package me.proxer.app.manga

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import me.proxer.app.GlideRequests
import me.proxer.app.util.extension.decodedName
import me.proxer.app.util.extension.logErrors
import me.proxer.app.util.wrapper.OriginalSizeGlideTarget
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.util.ProxerUrls
import timber.log.Timber
import java.io.File

/**
 * @author Ruben Gees
 */
class MangaPreloader {

    private companion object {
        private const val PARALLEL_DOWNLOADS = 2
        private const val MAX_RETRIES = 3
    }

    var glide: GlideRequests? = null

    private val preloadTargets = mutableListOf<Target<File>>()
    private val pendingLinks = ArrayDeque<String>()

    /**
     * Preloads all pages of the passed [chapter], beginning with the page at [startPosition] (as the user is most
     * likely to read the pages after the current one next) and wrapping around to the pages before it.
     */
    fun preload(chapter: Chapter, startPosition: Int = 0) {
        cancel()

        val preloadList = chapter.pages
            ?.map { ProxerUrls.mangaPageImage(chapter.server, chapter.entryId, chapter.id, it.decodedName).toString() }
            ?: emptyList()

        val safeStartPosition = startPosition.coerceIn(0, (preloadList.size - 1).coerceAtLeast(0))

        pendingLinks += preloadList.drop(safeStartPosition)
        pendingLinks += preloadList.take(safeStartPosition)

        repeat(PARALLEL_DOWNLOADS) { preloadNext() }
    }

    fun cancel() {
        pendingLinks.clear()

        preloadTargets.forEach { glide?.clear(it) }
        preloadTargets.clear()
    }

    private fun preloadNext() {
        val next = pendingLinks.removeFirstOrNull()

        if (next != null) {
            preload(next)
        }
    }

    private fun preload(link: String, failures: Int = 0) {
        Timber.d("Preloading $link")

        val target = GlidePreloadTarget(link, failures)

        preloadTargets += target

        glide
            ?.downloadOnly()
            ?.load(link)
            ?.logErrors()
            ?.into(target)
    }

    internal inner class GlidePreloadTarget(
        private val link: String,
        private val failures: Int
    ) : OriginalSizeGlideTarget<File>() {

        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
            preloadNext()
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
            if (failures < MAX_RETRIES) {
                preload(link, failures + 1)
            } else {
                preloadNext()
            }
        }
    }
}
