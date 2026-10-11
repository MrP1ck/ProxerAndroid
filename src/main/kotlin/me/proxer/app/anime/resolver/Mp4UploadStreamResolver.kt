package me.proxer.app.anime.resolver

import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitString
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object Mp4UploadStreamResolver : StreamResolver() {

    private val packedRegex = Regex("return p.*?'(.*?)',(\\d+),(\\d+),'(.*?)'")
    private val urlRegex = Regex("player.src\\(\"(.*?)\"\\)")

    override val name = "MP4Upload"

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

        val packedFunction = packedRegex.find(body) ?: throw StreamResolutionException()

        val p = packedFunction.groupValues[1]
        val a = packedFunction.groupValues[2].toIntOrNull() ?: throw StreamResolutionException()
        val c = packedFunction.groupValues[3].toIntOrNull() ?: throw StreamResolutionException()
        val k = packedFunction.groupValues[4].split("|")

        if (p.isBlank() || k.size != c) {
            throw StreamResolutionException()
        }

        val unpacked = unpack(p, a, c, k)
        val urlRegexResult = urlRegex.find(unpacked) ?: throw StreamResolutionException()
        val videoUrl = urlRegexResult.groupValues[1].toPrefixedUrlOrNull() ?: throw StreamResolutionException()

        return StreamResolutionResult.Video(
            videoUrl,
            "video/mp4",
            referer = "https://www.mp4upload.com/",
            internalPlayerOnly = true
        )
    }

    private fun unpack(p: String, a: Int, c: Int, k: List<String>): String {
        return (c - 1 downTo 0).fold(
            p,
            { acc, next ->
                if (k[next].isNotEmpty()) {
                    acc.replace(Regex("\\b" + next.toString(a) + "\\b"), k[next])
                } else {
                    acc
                }
            }
        )
    }
}
