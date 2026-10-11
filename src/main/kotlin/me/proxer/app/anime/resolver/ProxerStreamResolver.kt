package me.proxer.app.anime.resolver

import android.net.Uri
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitString
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object ProxerStreamResolver : StreamResolver() {

    private val regex = Regex("<source type=\"(.*?)\" src=\"(.*?)\">")

    override val name = "Proxer-Stream"

    override suspend fun resolve(id: String): StreamResolutionResult {
        val (link, adTag) = api.anime.vastLink(id).await()

        val body = client
            .newCall(
                Request.Builder()
                    .get()
                    .url(link.toPrefixedUrlOrNull() ?: throw StreamResolutionException())
                    .header("User-Agent", USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
            .awaitString()

        val regexResult = regex.find(body) ?: throw StreamResolutionException()
        val url = regexResult.groupValues[2].toHttpUrlOrNull() ?: throw StreamResolutionException()
        val type = regexResult.groupValues[1]
        val adTagUri = if (adTag.isNotBlank()) Uri.parse(adTag) else null

        return StreamResolutionResult.Video(url, type, adTag = adTagUri)
    }
}
