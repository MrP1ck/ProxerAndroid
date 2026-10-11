package me.proxer.app.anime.resolver

import android.net.Uri
import me.proxer.app.MainApplication
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitString
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object ProxerStreamCFResolver : StreamResolver() {

    private val regex = Regex("<stream.*?src=\"(.*?)\".*?>")

    override val name = "Proxer-Stream (CF)"

    override suspend fun resolve(id: String): StreamResolutionResult {
        val (link, adTag) = api.anime.vastLink(id).await()

        val body = client
            .newCall(
                Request.Builder()
                    .get()
                    .url(link.toPrefixedUrlOrNull() ?: throw StreamResolutionException())
                    .header("User-Agent", MainApplication.USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
            .awaitString()

        val regexResult = regex.find(body) ?: throw StreamResolutionException()
        val streamId = regexResult.groupValues[1]
        val url = "https://videodelivery.net/$streamId/manifest/video.mpd".toHttpUrlOrNull()
            ?: throw StreamResolutionException()
        val adTagUri = if (adTag.isNotBlank()) Uri.parse(adTag) else null

        return StreamResolutionResult.Video(
            url,
            // Technically this should be application/dash+xml,
            // but other applications do not seem to support that.
            "application/x-mpegURL",
            adTag = adTagUri
        )
    }
}
