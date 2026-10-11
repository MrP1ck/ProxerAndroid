package me.proxer.app.media.person

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.EntryPerson

/**
 * The persons involved in an entry, like the voice actors and the author.
 */
class EntryPersonsViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String
) : BaseContentViewModel<List<EntryPerson>>(dependencies) {

    override val endpoint: Endpoint<List<EntryPerson>>
        get() = api.info.entryPersons(entryId)
}
