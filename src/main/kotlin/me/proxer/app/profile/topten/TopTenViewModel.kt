package me.proxer.app.profile.topten

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.profile.topten.TopTenViewModel.ZippedTopTenResult
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.toLocalEntry
import me.proxer.app.util.extension.toLocalEntryUcp
import me.proxer.library.enums.Category

/**
 * @author Ruben Gees
 */
class TopTenViewModel(
    dependencies: ViewModelDependencies,
    private val userId: String?,
    private val username: String?
) : BaseViewModel<ZippedTopTenResult>(dependencies) {

    override suspend fun loadData(): ZippedTopTenResult {
        validate()

        return when (storageHelper.user?.matches(userId, username) == true) {
            true -> {
                val entries = api.ucp.topTen().await().map { it.toLocalEntryUcp() }

                ZippedTopTenResult(
                    animeEntries = entries.filter { entry -> entry.category == Category.ANIME },
                    mangaEntries = entries.filter { entry -> entry.category == Category.MANGA }
                )
            }
            false -> coroutineScope {
                val includeHentai = preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn
                val animeEntries = async { loadPartial(includeHentai, Category.ANIME) }
                val mangaEntries = async { loadPartial(includeHentai, Category.MANGA) }

                ZippedTopTenResult(animeEntries.await(), mangaEntries.await())
            }
        }
    }

    val itemDeletionError = Events<ErrorUtils.ErrorAction>()

    private val deletionQueue = UniqueQueue<LocalTopTenEntry>()
    private var deletionJob: Job? = null

    init {
        viewModelScope.launch { storageHelper.isLoggedInChanges.collect { reload() } }
    }

    fun addItemToDelete(item: LocalTopTenEntry) {
        deletionQueue.add(item)

        if (deletionJob?.isActive != true) {
            startItemDeletion()
        }
    }

    private suspend fun loadPartial(includeHentai: Boolean, category: Category) = api.user.topTen(userId, username)
        .includeHentai(includeHentai)
        .category(category)
        .await()
        .map { entries -> entries.toLocalEntry() }

    private fun startItemDeletion() {
        deletionJob = viewModelScope.launch {
            while (true) {
                val item = deletionQueue.poll() ?: break
                val result = catchingErrors { api.ucp.deleteFavorite(item.id).awaitNullable() }

                result.onSuccess {
                    data.value = data.value?.let { currentData ->
                        ZippedTopTenResult(
                            currentData.animeEntries.filterNot { newItem -> newItem == item },
                            currentData.mangaEntries.filterNot { newItem -> newItem == item }
                        )
                    }
                }

                result.onFailure {
                    deletionQueue.clear()
                    itemDeletionError.send(ErrorUtils.handle(it))
                }
            }
        }
    }

    data class ZippedTopTenResult(val animeEntries: List<LocalTopTenEntry>, val mangaEntries: List<LocalTopTenEntry>)
}
