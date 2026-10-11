package me.proxer.app.base

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import me.proxer.app.TestApplication
import me.proxer.app.exception.NotConnectedException
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pins down the behavior of [BaseViewModel] that the screens rely on, independent of how it is implemented.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class BaseViewModelTest {

    @get:Rule
    val environment = ViewModelTestRule()

    private var loads = 0

    @Before
    fun setUp() {
        loads = 0
        TestViewModel.source = { Single.fromCallable { "data ${++loads}" } }
        TestViewModel.isLoginRequired = false
        TestViewModel.isAgeConfirmationRequired = false
    }

    @Test
    fun `load publishes the data`() {
        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()

        assertEquals("data 1", viewModel.data.value)
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `load publishes a handled error and clears the data`() {
        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()

        TestViewModel.source = { Single.error(IllegalStateException("broken")) }
        viewModel.load()
        idleMainLooper()

        assertNull(viewModel.data.value)
        assertNotNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `is loading until the request finishes and the previous data is cleared meanwhile`() {
        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()

        val pending = SingleSubject.create<String>()
        TestViewModel.source = { pending }

        viewModel.load()
        idleMainLooper()

        assertEquals(true, viewModel.isLoading.value)
        assertNull(viewModel.data.value)

        pending.onSuccess("done")
        idleMainLooper()

        assertEquals(false, viewModel.isLoading.value)
        assertEquals("done", viewModel.data.value)
    }

    @Test
    fun `a newer load replaces a pending one`() {
        val viewModel = TestViewModel()
        val first = SingleSubject.create<String>()

        TestViewModel.source = { first }
        viewModel.load()
        idleMainLooper()

        TestViewModel.source = { Single.just("second") }
        viewModel.load()
        idleMainLooper()

        first.onSuccess("first")
        idleMainLooper()

        assertEquals("second", viewModel.data.value)
        assertFalse(first.hasObservers())
    }

    @Test
    fun `loadIfPossible does not load while loading`() {
        val viewModel = TestViewModel()
        val pending = SingleSubject.create<String>()

        TestViewModel.source = {
            loads++
            pending
        }

        viewModel.load()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(1, loads)
    }

    @Test
    fun `loadIfPossible does not load again after an error`() {
        val viewModel = TestViewModel()

        TestViewModel.source = {
            loads++
            Single.error(IllegalStateException())
        }

        viewModel.load()
        idleMainLooper()
        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals(1, loads)
    }

    @Test
    fun `loadIfPossible loads when idle`() {
        val viewModel = TestViewModel()

        viewModel.loadIfPossible()
        idleMainLooper()

        assertEquals("data 1", viewModel.data.value)
    }

    @Test
    fun `reload clears the error and loads again`() {
        val viewModel = TestViewModel()

        TestViewModel.source = { Single.error(IllegalStateException()) }
        viewModel.load()
        idleMainLooper()
        assertNotNull(viewModel.error.value)

        TestViewModel.source = { Single.just("fixed") }
        viewModel.reload()
        idleMainLooper()

        assertNull(viewModel.error.value)
        assertEquals("fixed", viewModel.data.value)
    }

    @Test
    fun `login required views fail with a login action while logged out`() {
        TestViewModel.isLoginRequired = true
        TestViewModel.source = { Single.fromCallable { requireLogin() } }

        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()

        assertEquals(ButtonAction.LOGIN, viewModel.error.value?.buttonAction)
    }

    @Test
    fun `logging in reloads a login required view`() {
        TestViewModel.isLoginRequired = true
        TestViewModel.source = { Single.fromCallable { requireLogin() } }

        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()
        assertEquals(ButtonAction.LOGIN, viewModel.error.value?.buttonAction)

        environment.logIn()
        idleMainLooper()

        assertNull(viewModel.error.value)
        assertEquals("secret", viewModel.data.value)
    }

    @Test
    fun `logging in does not reload views which do not require it`() {
        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()
        environment.logIn()
        idleMainLooper()

        assertEquals(1, loads)
    }

    @Test
    fun `confirming the age reloads views which require it`() {
        TestViewModel.isAgeConfirmationRequired = true

        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()
        environment.preferenceHelper.isAgeRestrictedMediaAllowed = true
        idleMainLooper()

        assertEquals("data 2", viewModel.data.value)
    }

    @Test
    fun `a restored network connection retries a network error`() {
        val viewModel = TestViewModel()

        TestViewModel.source = { Single.error(NotConnectedException()) }
        viewModel.load()
        idleMainLooper()
        assertEquals(ButtonAction.NETWORK_SETTINGS, viewModel.error.value?.buttonAction)

        TestViewModel.source = { Single.just("online") }
        environment.bus.post(NetworkConnectedEvent())
        idleMainLooper()

        assertEquals("online", viewModel.data.value)
    }

    @Test
    fun `a restored network connection is ignored without a network error`() {
        val viewModel = TestViewModel()

        viewModel.load()
        idleMainLooper()
        environment.bus.post(NetworkConnectedEvent())
        idleMainLooper()

        assertEquals(1, loads)
    }

    @Test
    fun `a closed view model stops its request`() {
        val viewModel = TestViewModel()
        val pending = SingleSubject.create<String>()

        TestViewModel.source = { pending }
        viewModel.load()
        idleMainLooper()
        assertTrue(pending.hasObservers())

        viewModel.clear()

        assertFalse(pending.hasObservers())
    }

    private fun requireLogin(): String {
        Validators(environment.storageHelper, environment.preferenceHelper).validateLogin()

        return "secret"
    }
}

/**
 * The state is read from companion fields because the base class reads the overridden members while it is still
 * being constructed.
 */
private class TestViewModel : BaseViewModel<String>() {

    companion object {
        var source: () -> Single<String> = { Single.never() }
        var isLoginRequired = false
        var isAgeConfirmationRequired = false
    }

    override val isLoginRequired get() = Companion.isLoginRequired
    override val isAgeConfirmationRequired get() = Companion.isAgeConfirmationRequired
    override val dataSingle get() = source()

    fun clear() = onCleared()
}
