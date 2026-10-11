package me.proxer.app.media.discussion

import me.proxer.app.base.BaseContentViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.info.ForumDiscussion

/**
 * @author Ruben Gees
 */
class DiscussionViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String
) : BaseContentViewModel<List<ForumDiscussion>>(dependencies) {

    override val endpoint: Endpoint<List<ForumDiscussion>>
        get() = api.info.forumDiscussions(entryId)
}
