package me.proxer.app.info.character

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Character

class CharacterViewModel(
    dependencies: ViewModelDependencies,
    private val characterId: String
) : BaseContentViewModel<Character>(dependencies) {

    override val endpoint: Endpoint<Character>
        get() = api.info.character(characterId)
}
