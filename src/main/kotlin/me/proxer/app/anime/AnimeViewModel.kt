package me.proxer.app.anime

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolverFactory
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.AppRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.exception.StreamResolutionException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.awaitPartial
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.isAgeRestricted
import me.proxer.app.util.extension.toAnimeStream
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class AnimeViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String,
    private val language: AnimeLanguage,
    episode: Int
) : BaseViewModel<AnimeStreamInfo>(dependencies) {

    private companion object {
        private val streamComparator = compareBy<AnimeStream, Int?>(nullsLast()) { it.resolutionResult?.let { 0 } }
    }

    override suspend fun loadData(): AnimeStreamInfo {
        validate()

        val entry = loadEntry()

        if (entry.isAgeRestricted) {
            if (!storageHelper.isLoggedIn) {
                throw NotLoggedInException()
            } else if (!preferenceHelper.isAgeRestrictedMediaAllowed) {
                throw AgeConfirmationRequiredException()
            }
        }

        return AnimeStreamInfo(entry.name, entry.episodeAmount, loadStreams(entry))
    }

    val resolutionResult = Events<StreamResolutionResult>()
    val resolutionError = Events<ErrorAction>()

    val userStateData = Events<Unit>()
    val userStateError = Events<ErrorAction>()

    var episode by Delegates.observable(episode) { _, old, new ->
        if (old != new) reload()
    }

    private var cachedEntryCore: EntryCore? = null

    private var resolverJob: Job? = null
    private var userStateJob: Job? = null

    init {
        viewModelScope.launch {
            storageHelper.isLoggedInChanges.collect {
                if (it && error.value?.buttonAction == ButtonAction.LOGIN) {
                    reload()
                }
            }
        }

        viewModelScope.launch {
            preferenceHelper.isAgeRestrictedMediaAllowedChanges.collect {
                if (error.value?.buttonAction == ButtonAction.AGE_CONFIRMATION) {
                    reload()
                }
            }
        }
    }

    override fun load() {
        resolverJob?.cancel()

        super.load()
    }

    fun resolve(stream: AnimeStream) {
        resolverJob?.cancel()

        val knownResult = stream.resolutionResult
        val resolver = when (knownResult) {
            null -> StreamResolverFactory.resolverFor(stream.hosterName) ?: throw StreamResolutionException()
            else -> null
        }

        resolverJob = viewModelScope.launch {
            isLoading.value = true

            catchingErrors { knownResult ?: withContext(Dispatchers.Default) { resolver?.resolve(stream.id) } }
                .onSuccess { result -> result?.let { resolutionResult.send(it) } }
                .onFailure {
                    resolutionError.send(
                        when (it) {
                            is AppRequiredException -> AppRequiredErrorAction(it.name, it.appPackage)
                            else -> ErrorUtils.handle(it)
                        }
                    )
                }

            if (isActive) isLoading.value = false
        }
    }

    fun markAsFinished() = updateUserState(api.info.markAsFinished(entryId))

    fun bookmark(episode: Int) = updateUserState(
        api.ucp.setBookmark(entryId, episode, language.toMediaLanguage(), Category.ANIME)
    )

    private suspend fun loadEntry(): EntryCore {
        return cachedEntryCore ?: api.info.entryCore(entryId).await().also { cachedEntryCore = it }
    }

    private suspend fun loadStreams(entry: EntryCore): List<AnimeStream> = coroutineScope {
        api.anime.streams(entryId, episode, language)
            .includeProxerStreams(true)
            .awaitPartial(entry)
            .filterNot { stream -> StreamResolverFactory.resolverFor(stream.hosterName)?.ignore == true }
            .groupBy { stream -> stream.hoster }
            .map { (_, streams) -> streams.shuffled().first() }
            .map { stream ->
                async {
                    val resolver = StreamResolverFactory.resolverFor(stream.hosterName)

                    if (resolver != null && resolver.resolveEarly) {
                        val resolutionResult = withContext(Dispatchers.Default) { resolver.resolve(stream.id) }

                        stream.toAnimeStream(true, resolutionResult)
                    } else {
                        stream.toAnimeStream(resolver != null)
                    }
                }
            }
            .awaitAll()
            .sortedWith(streamComparator)
    }

    @Suppress("ForbiddenVoid")
    private fun updateUserState(endpoint: Endpoint<Unit?>) {
        userStateJob?.cancel()
        userStateJob = viewModelScope.launch {
            catchingErrors {
                validators.validateLogin()

                endpoint.awaitNullable()
            }
                .onSuccess { userStateData.send() }
                .onFailure { userStateError.send(ErrorUtils.handle(it)) }
        }
    }
}
