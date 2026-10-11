package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.isPackageInstalled
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toPrefixedUrlOrNull

/**
 * @author Ruben Gees
 */
object NetflixStreamResolver : StreamResolver() {

    private const val NETFLIX_PACKAGE = "com.netflix.mediaclient"

    override val name = "Netflix"

    private val packageManager by safeInject<PackageManager>()

    override suspend fun resolve(id: String): StreamResolutionResult {
        if (!packageManager.isPackageInstalled(NETFLIX_PACKAGE)) {
            throw AppRequiredException(name, NETFLIX_PACKAGE)
        }

        val url = api.anime.link(id).await().toPrefixedUrlOrNull() ?: throw StreamResolutionException()

        return StreamResolutionResult.App(url.androidUri())
    }
}
