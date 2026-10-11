package me.proxer.app.discover

import me.proxer.app.base.PagedViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.util.extension.await
import me.proxer.library.util.ProxerUrls

/**
 * Loads the pages of a [DirectoryKind], optionally filtered by the first letter or a search query.
 */
class DirectoryViewModel(
    dependencies: ViewModelDependencies,
    private val kind: DirectoryKind
) : PagedViewModel<DirectoryEntry>(dependencies) {

    override val itemsOnPage = 50

    /** The first letter to filter by, which is ignored while a [searchQuery] is set. */
    var letter: String? = null
        private set

    var searchQuery: String? = null
        private set

    /**
     * Applies the [letter] and [searchQuery] and reloads the data if they changed.
     */
    fun updateFilter(letter: String?, searchQuery: String?) {
        val newQuery = searchQuery?.trim()?.takeIf { it.isNotEmpty() }

        if (this.letter != letter || this.searchQuery != newQuery) {
            this.letter = letter
            this.searchQuery = newQuery

            reload()
        }
    }

    override fun areItemsTheSame(old: DirectoryEntry, new: DirectoryEntry) = old.id == new.id

    override suspend fun loadData(): List<DirectoryEntry> {
        validate()

        val start = letter.takeIf { searchQuery == null }

        return when (kind) {
            DirectoryKind.CHARACTERS -> api.list.characterList()
                .searchStart(start)
                .search(searchQuery)
                .page(page)
                .limit(itemsOnPage)
                .await()
                .map { DirectoryEntry(it.id, it.name, ProxerUrls.characterImage(it.id)) }
            DirectoryKind.PERSONS -> api.list.personList()
                .searchStart(start)
                .search(searchQuery)
                .page(page)
                .limit(itemsOnPage)
                .await()
                .map { DirectoryEntry(it.id, it.name, ProxerUrls.personImage(it.id)) }
            DirectoryKind.TRANSLATOR_GROUPS -> api.list.translatorGroupList()
                .searchStart(start)
                .search(searchQuery)
                .page(page)
                .limit(itemsOnPage)
                .await()
                .map { DirectoryEntry(it.id, it.name, ProxerUrls.translatorGroupImage(it.id)) }
            DirectoryKind.INDUSTRIES -> api.list.industryList()
                .searchStart(start)
                .search(searchQuery)
                .page(page)
                .limit(itemsOnPage)
                .await()
                .map { DirectoryEntry(it.id, it.name, ProxerUrls.industryImage(it.id), it.type) }
        }
    }
}
