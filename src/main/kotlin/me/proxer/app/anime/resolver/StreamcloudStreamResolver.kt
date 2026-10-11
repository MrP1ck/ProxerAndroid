package me.proxer.app.anime.resolver

import me.proxer.app.MainApplication.Companion.GENERIC_USER_AGENT
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitString
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * @author Ruben Gees
 */
object StreamcloudStreamResolver : StreamResolver() {

    private val formRegex = Regex("<input.*?name=\"(.*?)\".*?value=\"(.*?)\">")
    private val fileRegex = Regex("file: \"(.+?)\",")

    override val name = "Streamcloud"

    override suspend fun resolve(id: String): StreamResolutionResult {
        val url = api.anime.link(id).await()

        val formBody = client
            .newCall(
                Request.Builder()
                    .get()
                    .url(url)
                    .header("User-Agent", GENERIC_USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
            .awaitString()

        val formValues = FormBody.Builder()
            .apply {
                for (i in formRegex.findAll(formBody)) {
                    add(i.groupValues[1], i.groupValues[2].replace("download1", "download2"))
                }
            }
            .build()

        if (formValues.size == 0) {
            throw StreamResolutionException()
        }

        val fileBody = client
            .newCall(
                Request.Builder()
                    .post(formValues)
                    .url(url)
                    .header("User-Agent", GENERIC_USER_AGENT)
                    .header("Connection", "close")
                    .build()
            )
            .awaitString()

        val fileUrl = fileRegex.find(fileBody)?.groupValues?.get(1)?.toHttpUrlOrNull()
            ?: throw StreamResolutionException()

        return StreamResolutionResult.Video(fileUrl, "video/mp4", url, internalPlayerOnly = true)
    }
}
