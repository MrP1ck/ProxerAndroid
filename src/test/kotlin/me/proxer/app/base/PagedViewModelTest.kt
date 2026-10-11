package me.proxer.app.base

import io.reactivex.Single
import me.proxer.app.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins down the paging, refreshing and merging behavior of [PagedViewModel].
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class PagedViewModelTest {

    @get:Rule
    val environment = ViewModelTestRule()

    private val requestedPages = mutableListOf<Int>()

    @Before
    fun setUp() {
        requestedPages.clear()

        PagedTestViewModel.source = { page ->
            requestedPages += page

            Single.just(List(PagedTestViewModel.ITEMS_ON_PAGE) { page * PagedTestViewModel.ITEMS_ON_PAGE + it })
        }
    }

    @Test
    fun `the first load publishes the first page`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()

        assertEquals(listOf(0, 1, 2), viewModel.data.value)
        assertEquals(listOf(0), requestedPages)
    }

    @Test
    fun `loading more appends the next page`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(listOf(0, 1, 2, 3, 4, 5), viewModel.data.value)
        assertEquals(listOf(0, 1), requestedPages)
    }

    @Test
    fun `a short page marks the end and stops loading more`() {
        val viewModel = PagedTestViewModel()

        PagedTestViewModel.source = { page ->
            requestedPages += page
            Single.just(listOf(0, 1))
        }
        viewModel.load()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(listOf(0, 1), viewModel.data.value)
        assertEquals(listOf(0), requestedPages)
    }

    @Test
    fun `loading more skips items which are already shown`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()

        PagedTestViewModel.source = { page ->
            requestedPages += page
            Single.just(listOf(2, 3, 4))
        }
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(listOf(0, 1, 2, 3, 4), viewModel.data.value)
    }

    @Test
    fun `refreshing requests the first page and puts new items first`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        PagedTestViewModel.source = { page ->
            requestedPages += page
            Single.just(listOf(100, 0, 1))
        }
        viewModel.refresh()
        idleMainLooper()

        assertEquals(listOf(100, 0, 1, 2, 3, 4, 5), viewModel.data.value)
        assertEquals(0, requestedPages.last())
    }

    @Test
    fun `refreshing does not advance the page`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()
        viewModel.refresh()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(listOf(0, 0, 1), requestedPages)
    }

    @Test
    fun `an error on the first page is a blocking error`() {
        val viewModel = PagedTestViewModel()

        PagedTestViewModel.source = { Single.error(IllegalStateException()) }
        viewModel.load()
        idleMainLooper()

        assertNotNull(viewModel.error.value)
        assertNull(viewModel.refreshError.value)
        assertNull(viewModel.data.value)
    }

    @Test
    fun `a failing refresh keeps the data and reports a refresh error`() {
        val viewModel = PagedTestViewModel()
        val refreshErrors = mutableListOf<Any?>()

        viewModel.refreshError.observeForever { if (it != null) refreshErrors += it }

        viewModel.load()
        idleMainLooper()

        PagedTestViewModel.source = { Single.error(IllegalStateException()) }
        viewModel.refresh()
        idleMainLooper()

        assertEquals(listOf(0, 1, 2), viewModel.data.value)
        assertNull(viewModel.error.value)
        assertEquals(1, refreshErrors.size)
    }

    @Test
    fun `a failing page after the first one is a blocking error`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()

        PagedTestViewModel.source = { Single.error(IllegalStateException()) }
        viewModel.loadIfPossible()
        idleMainLooper()

        assertNotNull(viewModel.error.value)
        assertNull(viewModel.refreshError.value)
    }

    @Test
    fun `reload starts again at the first page`() {
        val viewModel = PagedTestViewModel()

        viewModel.load()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()
        viewModel.reload()
        idleMainLooper()

        assertEquals(listOf(0, 1, 2), viewModel.data.value)
        assertEquals(listOf(0, 1, 0), requestedPages)
    }

    @Test
    fun `reload allows loading more again after the end was reached`() {
        val viewModel = PagedTestViewModel()

        PagedTestViewModel.source = { page ->
            requestedPages += page
            Single.just(listOf(0))
        }
        viewModel.load()
        idleMainLooper()
        viewModel.reload()
        idleMainLooper()

        PagedTestViewModel.source = { page ->
            requestedPages += page
            Single.just(listOf(0, 1, 2))
        }
        viewModel.reload()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(listOf(0, 1), requestedPages.takeLast(2))
    }
}

private class PagedTestViewModel : PagedViewModel<Int>() {

    companion object {
        const val ITEMS_ON_PAGE = 3

        var source: (page: Int) -> Single<List<Int>> = { Single.never() }
    }

    override val isLoginRequired = false
    override val itemsOnPage = ITEMS_ON_PAGE
    override val dataSingle get() = source(page)
}
