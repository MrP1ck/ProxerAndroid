package me.proxer.app.media.recommendation

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.Recommendation

/**
 * @author Ruben Gees
 */
class RecommendationViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String
) : BaseContentViewModel<List<Recommendation>>(dependencies) {

    override val endpoint: Endpoint<List<Recommendation>>
        get() = api.info.recommendations(entryId)
}
