package me.proxer.app.info.industry

import me.proxer.app.base.PagedContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.list.IndustryProject

/**
 * @author Ruben Gees
 */
class IndustryProjectViewModel(
    dependencies: ViewModelDependencies,
    private val industryId: String
) : PagedContentViewModel<IndustryProject>(dependencies) {

    override val itemsOnPage = 30

    override val endpoint: PagingLimitEndpoint<List<IndustryProject>>
        get() = api.list.industryProjectList(industryId)
            .includeHentai(preferenceHelper.isAgeRestrictedMediaAllowed && storageHelper.isLoggedIn)
}
