package me.proxer.app.base

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import me.proxer.app.TestApplication
import me.proxer.app.exception.NotConnectedException
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Validators
import org.junit.Assert.assertEquals
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
        TestViewModel.source = { "data ${++loads}" }
        TestViewModel.isLoginRequired = false
        TestViewModel.isAgeConfirmationRequired = false
    }

    @Test
    fun `load publishes the data`() {
        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()

        assertEquals("data 1", viewModel.data.value)
        assertNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `load publishes a handled error and clears the data`() {
        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()

        TestViewModel.source = { error("broken") }
        viewModel.load()

        assertNull(viewModel.data.value)
        assertNotNull(viewModel.error.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `is loading until the request finishes and the previous data is cleared meanwhile`() {
        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()

        val pending = CompletableDeferred<String>()
        TestViewModel.source = { pending.await() }

        viewModel.load()

        assertEquals(true, viewModel.isLoading.value)
        assertNull(viewModel.data.value)

        pending.complete("done")

        assertEquals(false, viewModel.isLoading.value)
        assertEquals("done", viewModel.data.value)
    }

    @Test
    fun `a newer load replaces a pending one`() {
        val viewModel = TestViewModel(environment.dependencies)
        val first = CompletableDeferred<String>()

        TestViewModel.source = { first.await() }
        viewModel.load()

        TestViewModel.source = { "second" }
        viewModel.load()

        first.complete("first")

        assertEquals("second", viewModel.data.value)
        assertEquals(false, viewModel.isLoading.value)
    }

    @Test
    fun `loadIfPossible does not load while loading`() {
        val viewModel = TestViewModel(environment.dependencies)
        val pending = CompletableDeferred<String>()

        TestViewModel.source = {
            loads++
            pending.await()
        }

        viewModel.load()
        viewModel.loadIfPossible()

        assertEquals(1, loads)
    }

    @Test
    fun `loadIfPossible does not load again after an error`() {
        val viewModel = TestViewModel(environment.dependencies)

        TestViewModel.source = {
            loads++
            error("broken")
        }

        viewModel.load()
        viewModel.loadIfPossible()

        assertEquals(1, loads)
    }

    @Test
    fun `loadIfPossible loads when idle`() {
        val viewModel = TestViewModel(environment.dependencies)

        viewModel.loadIfPossible()

        assertEquals("data 1", viewModel.data.value)
    }

    @Test
    fun `reload clears the error and loads again`() {
        val viewModel = TestViewModel(environment.dependencies)

        TestViewModel.source = { throw IllegalStateException() }
        viewModel.load()
        assertNotNull(viewModel.error.value)

        TestViewModel.source = { "fixed" }
        viewModel.reload()

        assertNull(viewModel.error.value)
        assertEquals("fixed", viewModel.data.value)
    }

    @Test
    fun `login required views fail with a login action while logged out`() {
        TestViewModel.isLoginRequired = true
        TestViewModel.source = { requireLogin() }

        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()

        assertEquals(ButtonAction.LOGIN, viewModel.error.value?.buttonAction)
    }

    @Test
    fun `logging in reloads a login required view`() {
        TestViewModel.isLoginRequired = true
        TestViewModel.source = { requireLogin() }

        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()
        assertEquals(ButtonAction.LOGIN, viewModel.error.value?.buttonAction)

        environment.logIn()

        assertNull(viewModel.error.value)
        assertEquals("secret", viewModel.data.value)
    }

    @Test
    fun `logging in reloads every view because the content can depend on the account`() {
        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()
        environment.logIn()

        assertEquals("data 2", viewModel.data.value)
    }

    @Test
    fun `views which do not require a login load while logged out`() {
        TestViewModel.source = { "secret" }

        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()

        assertNull(viewModel.error.value)
        assertEquals("secret", viewModel.data.value)
    }

    @Test
    fun `confirming the age reloads views which require it`() {
        TestViewModel.isAgeConfirmationRequired = true

        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()
        assertEquals(ButtonAction.AGE_CONFIRMATION, viewModel.error.value?.buttonAction)

        environment.preferenceHelper.isAgeRestrictedMediaAllowed = true

        assertNull(viewModel.error.value)
        assertEquals("data 1", viewModel.data.value)
    }

    @Test
    fun `a restored network connection retries a network error`() {
        val viewModel = TestViewModel(environment.dependencies)

        TestViewModel.source = { throw NotConnectedException() }
        viewModel.load()
        assertEquals(ButtonAction.NETWORK_SETTINGS, viewModel.error.value?.buttonAction)

        TestViewModel.source = { "online" }
        environment.bus.post(NetworkConnectedEvent())

        assertEquals("online", viewModel.data.value)
    }

    @Test
    fun `a restored network connection is ignored without a network error`() {
        val viewModel = TestViewModel(environment.dependencies)

        viewModel.load()
        environment.bus.post(NetworkConnectedEvent())

        assertEquals(1, loads)
    }

    @Test
    fun `a closed view model stops its request`() {
        val viewModel = TestViewModel(environment.dependencies)
        var wasCancelled = false

        TestViewModel.source = {
            try {
                awaitCancellation()
            } finally {
                wasCancelled = true
            }
        }
        viewModel.load()
        assertEquals(true, viewModel.isLoading.value)

        viewModel.viewModelScope.cancel()

        assertTrue(wasCancelled)
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
private class TestViewModel(dependencies: ViewModelDependencies) : BaseViewModel<String>(dependencies) {

    companion object {
        var source: suspend () -> String = { awaitCancellation() }
        var isLoginRequired = false
        var isAgeConfirmationRequired = false
    }

    override val isLoginRequired get() = Companion.isLoginRequired
    override val isAgeConfirmationRequired get() = Companion.isAgeConfirmationRequired

    override suspend fun loadData(): String {
        validate()

        return source()
    }
}
