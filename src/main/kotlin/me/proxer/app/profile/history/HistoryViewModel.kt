package me.proxer.app.profile.history

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.proxer.app.base.PagedViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.toLocalEntry
import me.proxer.app.util.extension.toLocalEntryUcp

/**
 * @author Ruben Gees
 */
class HistoryViewModel(
    dependencies: ViewModelDependencies,
    private val userId: String?,
    private val username: String?
) : PagedViewModel<LocalUserHistoryEntry>(dependencies) {

    override val itemsOnPage = 50

    override suspend fun loadData(): List<LocalUserHistoryEntry> {
        validate()

        return when (storageHelper.user?.matches(userId, username) == true) {
            true ->
                api.ucp.history()
                    .page(page)
                    .limit(itemsOnPage)
                    .await()
                    .map { it.toLocalEntryUcp() }
            false ->
                api.user.history(userId, username)
                    .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
                    .page(page)
                    .limit(itemsOnPage)
                    .await()
                    .map { it.toLocalEntry() }
        }
    }

    init {
        viewModelScope.launch { storageHelper.isLoggedInChanges.collect { reload() } }
    }
}
