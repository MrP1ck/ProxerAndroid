package me.proxer.app.news

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.proxer.app.base.PagedContentViewModel
import me.proxer.library.api.PagingLimitEndpoint
import me.proxer.library.entity.notifications.NewsArticle

/**
 * @author Ruben Gees
 */
class NewsViewModel : PagedContentViewModel<NewsArticle>() {

    override val itemsOnPage = 15

    override suspend fun loadData(): List<NewsArticle> = super.loadData().also {
        if (page == 0) {
            it.firstOrNull()?.date?.toInstant()?.let { date ->
                preferenceHelper.lastNewsDate = date
            }
        }
    }

    override val endpoint: PagingLimitEndpoint<List<NewsArticle>>
        get() = api.notifications.news()
            .markAsRead(page == 0)

    init {
        // Being registered tells the notification worker that the news are visible.
        viewModelScope.launch { bus.events<NewsNotificationEvent>().collect { } }
    }
}
