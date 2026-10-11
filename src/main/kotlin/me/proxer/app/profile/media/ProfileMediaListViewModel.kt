package me.proxer.app.profile.media

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.toLocalEntry
import me.proxer.app.util.extension.toLocalEntryUcp
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ProfileMediaListViewModel(
    private val userId: String?,
    private val username: String?,
    category: Category,
    filter: UserMediaListFilterType?
) : PagedViewModel<LocalUserMediaListEntry>() {

    override val itemsOnPage = 30

    override suspend fun loadData(): List<LocalUserMediaListEntry> {
        validate()

        val includeHentai = preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn

        return when (storageHelper.user?.matches(userId, username) == true) {
            true ->
                api.ucp.mediaList()
                    .includeHentai(includeHentai)
                    .category(category)
                    .filter(filter)
                    .page(page)
                    .limit(itemsOnPage)
                    .await()
                    .map { it.toLocalEntryUcp() }
            false ->
                api.user.mediaList(userId, username)
                    .includeHentai(includeHentai)
                    .category(category)
                    .filter(filter)
                    .page(page)
                    .limit(itemsOnPage)
                    .await()
                    .map { it.toLocalEntry() }
        }
    }

    val itemDeletionError = Events<ErrorUtils.ErrorAction>()

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    var filter by Delegates.observable(filter) { _, old, new ->
        if (old != new) reload()
    }

    private val deletionQueue = UniqueQueue<LocalUserMediaListEntry>()
    private var deletionJob: Job? = null

    init {
        viewModelScope.launch { storageHelper.isLoggedInChanges.collect { reload() } }
    }

    fun addItemToDelete(item: LocalUserMediaListEntry) {
        deletionQueue.add(item)

        if (deletionJob?.isActive != true) {
            startItemDeletion()
        }
    }

    private fun startItemDeletion() {
        deletionJob = viewModelScope.launch {
            while (true) {
                val item = deletionQueue.poll() ?: break
                val result = catchingErrors { api.ucp.deleteComment(item.commentId).awaitNullable() }

                result.onSuccess { data.value = data.value?.filterNot { newItem -> newItem == item } }

                result.onFailure {
                    deletionQueue.clear()
                    itemDeletionError.send(ErrorUtils.handle(it))
                }
            }
        }
    }
}
