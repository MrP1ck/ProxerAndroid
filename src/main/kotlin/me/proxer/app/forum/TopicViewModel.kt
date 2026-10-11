package me.proxer.app.forum

import android.content.res.Resources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import me.proxer.app.base.PagedViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.toParsedPost
import me.proxer.app.util.extension.toTopicMetaData

/**
 * @author Ruben Gees
 */
class TopicViewModel(
    dependencies: ViewModelDependencies,
    private val id: String,
    private val resources: Resources
) : PagedViewModel<ParsedPost>(dependencies) {

    override val itemsOnPage = 10

    override suspend fun loadData(): List<ParsedPost> {
        validate()

        val topic = api.forum.topic(id)
            .page(page)
            .limit(itemsOnPage)
            .await()

        metaData.value = topic.toTopicMetaData()

        return withContext(Dispatchers.Default) { topic.posts.map { post -> post.toParsedPost(resources) } }
    }

    val metaData = MutableStateFlow<TopicMetaData?>(null)
}
