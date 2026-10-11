package me.proxer.app.anime.resolver

import com.squareup.moshi.Moshi
import me.proxer.library.ProxerApi
import okhttp3.OkHttpClient
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Ruben Gees
 */
abstract class StreamResolver : KoinComponent {

    abstract val name: String

    open val resolveEarly: Boolean get() = false
    open val ignore: Boolean get() = false

    protected val api by inject<ProxerApi>()
    protected val client by inject<OkHttpClient>()
    protected val moshi by inject<Moshi>()

    open fun supports(name: String) = name.equals(this.name, true)
    abstract suspend fun resolve(id: String): StreamResolutionResult
}
