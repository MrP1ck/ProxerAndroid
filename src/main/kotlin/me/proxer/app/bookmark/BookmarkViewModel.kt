package me.proxer.app.bookmark

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.Category
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class BookmarkViewModel(
    searchQuery: String?,
    category: Category?,
    filterAvailable: Boolean
) : PagedContentViewModel<Bookmark>() {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<Bookmark>>
        get() = api.ucp.bookmarks()
            .name(searchQuery)
            .category(category)
            .filterAvailable(if (filterAvailable) true else null)

    val itemDeletionError = Events<ErrorUtils.ErrorAction>()
    val undoData = Events<Unit>()
    val undoError = Events<ErrorUtils.ErrorAction>()

    var searchQuery by Delegates.observable(searchQuery) { _, old, new ->
        if (old != new && !(old.isNullOrBlank() && new.isNullOrBlank())) reload()
    }

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    var filterAvailable by Delegates.observable(filterAvailable) { _, old, new ->
        if (old != new) reload()
    }

    private val deletionQueue = UniqueQueue<Bookmark>()
    private var deletionJob: Job? = null

    private var undoItem: Bookmark? = null
    private var undoJob: Job? = null

    override fun refresh() = reload()

    fun addItemToDelete(item: Bookmark) {
        deletionQueue.add(item)

        if (deletionJob?.isActive != true) {
            startItemDeletion()
        }
    }

    fun undo() {
        val safeUndoItem = undoItem

        if (safeUndoItem != null) {
            deletionJob?.cancel()
            undoJob?.cancel()
            undoJob = viewModelScope.launch {
                catchingErrors {
                    validators.validateLogin()

                    bookmarkCall(safeUndoItem).awaitNullable()
                }
                    .onSuccess { data.value = listOf(safeUndoItem) + (data.value ?: emptyList()) }
                    .onFailure { undoError.send(ErrorUtils.handle(it)) }
            }
        }
    }

    private fun startItemDeletion() {
        deletionJob = viewModelScope.launch {
            while (true) {
                val item = deletionQueue.poll() ?: break

                undoJob?.cancel()
                undoItem = null

                val result = catchingErrors { api.ucp.deleteBookmark(item.id).awaitNullable() }

                result.onSuccess {
                    undoItem = item
                    undoData.send()
                    data.value = data.value?.filterNot { newItem -> newItem == item }
                }

                result.onFailure {
                    deletionQueue.clear()
                    itemDeletionError.send(ErrorUtils.handle(it))
                }
            }
        }
    }

    private fun bookmarkCall(bookmark: Bookmark) = api.ucp
        .setBookmark(bookmark.entryId, bookmark.episode, bookmark.language, bookmark.category)
}
