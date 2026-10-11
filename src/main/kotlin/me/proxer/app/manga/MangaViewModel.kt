package me.proxer.app.manga

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.exception.PartialException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.awaitPartial
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.isAgeRestricted
import me.proxer.app.util.extension.isOfficial
import me.proxer.app.util.extension.toMediaLanguage
import me.proxer.app.util.extension.toPrefixedHttpUrl
import me.proxer.library.api.Endpoint
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class MangaViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String,
    private val language: Language,
    episode: Int
) : BaseViewModel<MangaChapterInfo>(dependencies) {

    override suspend fun loadData(): MangaChapterInfo {
        validate()

        val entry = api.info.entryCore(entryId).await()

        if (entry.isAgeRestricted && !preferenceHelper.isAgeRestrictedMediaAllowed) {
            throw AgeConfirmationRequiredException()
        }

        val chapter = api.manga.chapter(entryId, episode, language).awaitPartial(entry)

        if (!chapter.isOfficial && !storageHelper.isLoggedIn) {
            throw NotLoggedInException()
        }

        val data = MangaChapterInfo(chapter, entry.name, entry.episodeAmount)

        if (data.chapter.isOfficial) {
            throw PartialException(
                MangaLinkException(data.chapter.title, data.chapter.server.toPrefixedHttpUrl()),
                entry
            )
        } else if (data.chapter.pages == null) {
            throw PartialException(MangaNotAvailableException(), entry)
        }

        return data
    }

    val userStateData = Events<Unit>()
    val userStateError = Events<ErrorUtils.ErrorAction>()

    var episode by Delegates.observable(episode) { _, old, new ->
        if (old != new) reload()
    }

    private var userStateJob: Job? = null

    init {
        viewModelScope.launch {
            preferenceHelper.isAgeRestrictedMediaAllowedChanges.collect {
                if (error.value?.buttonAction == ButtonAction.AGE_CONFIRMATION) {
                    reload()
                }
            }
        }
    }

    fun setEpisode(value: Int, trigger: Boolean = true) {
        if (episode != value) {
            episode = value

            if (trigger) reload()
        }
    }

    fun markAsFinished() = updateUserState(api.info.markAsFinished(entryId))

    fun bookmark(episode: Int) = updateUserState(
        api.ucp.setBookmark(entryId, episode, language.toMediaLanguage(), Category.MANGA)
    )

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
