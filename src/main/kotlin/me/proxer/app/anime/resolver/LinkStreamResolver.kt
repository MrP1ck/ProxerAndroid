package me.proxer.app.anime.resolver

import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.toPrefixedUrlOrNull

/**
 * A resolver for hosters which can only be opened in the browser.
 *
 * @author Ruben Gees
 */
abstract class LinkStreamResolver : StreamResolver() {

    override suspend fun resolve(id: String): StreamResolutionResult {
        val url = api.anime.link(id).await().toPrefixedUrlOrNull() ?: throw StreamResolutionException()

        return StreamResolutionResult.Link(url)
    }
}
