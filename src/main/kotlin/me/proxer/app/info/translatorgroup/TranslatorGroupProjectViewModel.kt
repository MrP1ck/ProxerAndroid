package me.proxer.app.info.translatorgroup

import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.TranslatorGroupProject

/**
 * @author Ruben Gees
 */
class TranslatorGroupProjectViewModel(
    dependencies: ViewModelDependencies,
    private val translatorGroupId: String
) : PagedContentViewModel<TranslatorGroupProject>(dependencies) {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<TranslatorGroupProject>>
        get() = api.list.translatorGroupProjectList(translatorGroupId)
            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
}
