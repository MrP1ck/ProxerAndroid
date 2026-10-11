package me.proxer.app.anime.resolver

import android.content.pm.PackageManager
import android.net.Uri
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.isPackageInstalled
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Ruben Gees
 */
object CrunchyrollStreamResolver : StreamResolver(), KoinComponent {

    private const val CRUNCHYROLL_PACKAGE = "com.crunchyroll.crunchyroid"

    private val regex = Regex("media_id=(\\d*)?")

    override val name = "Crunchyroll"

    private val packageManager by inject<PackageManager>()

    override fun supports(name: String) = name.startsWith(this.name, true)

    override suspend fun resolve(id: String): StreamResolutionResult {
        if (!packageManager.isPackageInstalled(CRUNCHYROLL_PACKAGE)) {
            throw AppRequiredException(name, CRUNCHYROLL_PACKAGE)
        }

        val url = api.anime.link(id).await()
        val regexResult = regex.find(url) ?: throw StreamResolutionException()
        val mediaId = regexResult.groupValues[1]

        if (mediaId.isBlank()) {
            throw StreamResolutionException()
        }

        return StreamResolutionResult.App(Uri.parse("crunchyroll://media/${Uri.encode(mediaId)}"))
    }
}
