package me.proxer.app.info.translatorgroup

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.TranslatorGroup

/**
 * @author Ruben Gees
 */
class TranslatorGroupInfoViewModel(
    dependencies: ViewModelDependencies,
    private val translatorGroupId: String
) : BaseContentViewModel<TranslatorGroup>(dependencies) {

    override val endpoint: Endpoint<TranslatorGroup>
        get() = api.info.translatorGroup(translatorGroupId)
}
