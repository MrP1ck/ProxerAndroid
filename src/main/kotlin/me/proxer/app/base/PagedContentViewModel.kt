package me.proxer.app.base

import me.proxer.app.util.extension.await
import me.proxer.library.api.PagingLimitEndpoint

/**
 * @author Ruben Gees
 */
abstract class PagedContentViewModel<T> : PagedViewModel<T>() {

    override suspend fun loadData(): List<T> {
        validate()

        return endpoint.page(page).limit(itemsOnPage).await()
    }

    protected abstract val endpoint: PagingLimitEndpoint<List<T>>
}
