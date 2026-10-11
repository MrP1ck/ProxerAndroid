package me.proxer.app.anime.resolver

import androidx.core.text.parseAsHtml
import me.proxer.app.util.extension.await

/**
 * @author Ruben Gees
 */
object MessageStreamResolver : StreamResolver() {

    override val name = "Nachricht"
    override val resolveEarly = true

    override suspend fun resolve(id: String): StreamResolutionResult {
        return StreamResolutionResult.Message(api.anime.link(id).await().trim().parseAsHtml().trim())
    }
}
