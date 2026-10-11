package me.proxer.app.anime.resolver

import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitString
import me.proxer.app.util.extension.retryWithDelay
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object YourUploadStreamResolver : StreamResolver() {

    private const val HEAD_ATTEMPTS = 4

    private val regex = Regex("file: '(.*?)'")

    override val name = "YourUpload"

    override suspend fun resolve(id: String): StreamResolutionResult {
        val url = api.anime.link(id).await()

        val body = client
            .newCall(
                Request.Builder()
                    .get()
                    .url(url.toPrefixedUrlOrNull() ?: throw StreamResolutionException())
                    .header("User-Agent", GENERIC_USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
            .awaitString()

        val regexResult = regex.find(body) ?: throw StreamResolutionException()
        val fileUrl = regexResult.groupValues[1].toHttpUrlOrNull() ?: throw StreamResolutionException()

        val videoUrl = retryWithDelay(HEAD_ATTEMPTS, 0) {
            client
                .newCall(
                    Request.Builder()
                        .head()
                        .url(fileUrl)
                        .header("Referer", url)
                        .header("User-Agent", GENERIC_USER_AGENT)
                        .header("Connection", "close")
                        .build()
                )
                .await()
                .use { it.networkResponse?.request?.url }
        } ?: throw StreamResolutionException()

        return StreamResolutionResult.Video(videoUrl, "video/mp4", url, internalPlayerOnly = true)
    }
}
