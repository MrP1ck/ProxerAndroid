package me.proxer.app.media.episode

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage

/**
 * @author Ruben Gees
 */
class EpisodeViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String
) : BaseViewModel<List<EpisodeRow>>(dependencies) {

    override suspend fun loadData(): List<EpisodeRow> {
        validate()

        val info = api.info.episodeInfo(entryId)
            .limit(Int.MAX_VALUE)
            .await()

        return info.episodes
            .asSequence()
            .groupBy { it.number }
            .map { (_, episodes) -> EpisodeRow(info.category, info.userProgress, info.lastEpisode, episodes) }
            .toList()
            .sortedBy { it.number }
    }

    val bookmarkData = Events<Unit>()
    val bookmarkError = Events<ErrorUtils.ErrorAction>()

    private var bookmarkJob: Job? = null

    fun bookmark(episode: Int, language: MediaLanguage, category: Category) {
        bookmarkJob?.cancel()
        bookmarkJob = viewModelScope.launch {
            isLoading.value = true

            catchingErrors {
                validators.validateLogin()

                api.ucp.setBookmark(entryId, episode, language, category).awaitNullable()
            }
                .onSuccess { bookmarkData.send() }
                .onFailure { bookmarkError.send(ErrorUtils.handle(it)) }

            if (isActive) isLoading.value = false
        }
    }
}
