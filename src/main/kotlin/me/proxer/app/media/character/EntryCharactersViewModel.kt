package me.proxer.app.media.character

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.EntryCharacter

/**
 * The characters of an entry.
 */
class EntryCharactersViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String
) : BaseContentViewModel<List<EntryCharacter>>(dependencies) {

    override val endpoint: Endpoint<List<EntryCharacter>>
        get() = api.info.entryCharacters(entryId)
}
