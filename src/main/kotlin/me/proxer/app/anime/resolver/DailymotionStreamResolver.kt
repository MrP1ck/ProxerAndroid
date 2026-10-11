package me.proxer.app.anime.resolver

import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitString
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.util.regex.Pattern.quote

/**
 * @author Ruben Gees
 */
object DailymotionStreamResolver : StreamResolver() {

    private val regex = Regex("\"qualities\":(${quote("{")}.+${quote("}")}${quote("]")}${quote("}")}),")

    override val name = "Dailymotion"

    @Suppress("SwallowedException")
    override suspend fun resolve(id: String): StreamResolutionResult {
        val url = api.anime.link(id).await()

        val html = client
            .newCall(
                Request.Builder()
                    .get()
                    .url(url.toPrefixedUrlOrNull() ?: throw StreamResolutionException())
                    .header("User-Agent", GENERIC_USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
            .awaitString()

        val qualitiesJson = regex.find(html)?.value ?: throw StreamResolutionException()

        val qualityMap = try {
            moshi.adapter(DailymotionQualityMap::class.java)
                .fromJson("{${qualitiesJson.trimEnd(',')}}")
                ?: throw StreamResolutionException()
        } catch (error: JsonDataException) {
            throw StreamResolutionException()
        } catch (error: JsonEncodingException) {
            throw StreamResolutionException()
        }

        val link = qualityMap.qualities
            .flatMap { (quality, links) ->
                links.mapNotNull { (type, url) ->
                    url.toHttpUrlOrNull()?.let { DailymotionLinkWithQuality(quality, type, it) }
                }
            }
            .filter { it.type == "application/x-mpegURL" || it.type == "video/mp4" }
            .sortedWith(compareBy<DailymotionLinkWithQuality> { it.type }.thenByDescending { it.quality })
            .firstOrNull()
            ?: throw StreamResolutionException()

        return StreamResolutionResult.Video(link.url, link.type)
    }

    @JsonClass(generateAdapter = true)
    internal data class DailymotionQualityMap(val qualities: Map<String, Array<DailymotionLink>>)

    @JsonClass(generateAdapter = true)
    internal data class DailymotionLink(val type: String, val url: String)

    private data class DailymotionLinkWithQuality(val quality: String, val type: String, val url: HttpUrl)
}
