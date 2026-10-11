package me.proxer.app.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import timber.log.Timber

/**
 * @author Ruben Gees
 */
abstract class BaseViewModel<T>(dependencies: ViewModelDependencies) : ViewModel() {

    open val data = MutableStateFlow<T?>(null)
    open val error = MutableStateFlow<ErrorUtils.ErrorAction?>(null)
    open val isLoading = MutableStateFlow(false)

    /**
     * Whether the screen shows account data, which can not be loaded without being logged in. The other screens show
     * public content and work for guests as well.
     */
    protected open val isLoginRequired = false
    protected open val isAgeConfirmationRequired = false

    protected val bus = dependencies.bus
    protected val api = dependencies.api
    protected val storageHelper = dependencies.storageHelper
    protected val preferenceHelper = dependencies.preferenceHelper
    protected val validators = dependencies.validators

    protected var dataJob: Job? = null

    /**
     * Loads the data. Runs in [viewModelScope], so it must not block the main thread.
     */
    protected abstract suspend fun loadData(): T

    init {
        viewModelScope.launch {
            // What is shown can depend on the account (e.g. age restricted content), so every screen reloads.
            storageHelper.isLoggedInChanges.collect { reload() }
        }

        viewModelScope.launch {
            preferenceHelper.isAgeRestrictedMediaAllowedChanges.collect { if (isAgeConfirmationRequired) reload() }
        }

        viewModelScope.launch {
            bus.events<CaptchaSolvedEvent>().collect {
                if (error.value?.buttonAction == ButtonAction.CAPTCHA) {
                    refresh()
                }
            }
        }

        viewModelScope.launch {
            bus.events<NetworkConnectedEvent>().collect {
                if (error.value?.buttonAction == ButtonAction.NETWORK_SETTINGS) {
                    refresh()
                }
            }
        }
    }

    open fun load() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            isLoading.value = true
            error.value = null
            data.value = null

            try {
                val result = loadData()

                error.value = null
                data.value = result
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") loadError: Throwable) {
                Timber.e(loadError)

                data.value = null
                error.value = ErrorUtils.handle(loadError)
            } finally {
                // A cancelled load is replaced by a newer one, which is responsible for the state.
                if (isActive) isLoading.value = false
            }
        }
    }

    /**
     * Loads the data if nothing was requested yet, e.g. when the screen is shown for the first time.
     */
    fun loadIfNeeded() {
        if (!isLoading.value && data.value == null && error.value == null) {
            load()
        }
    }

    open fun loadIfPossible() {
        if (!isLoading.value && error.value == null) {
            load()
        }
    }

    open fun refresh() = load()

    open fun reload() {
        error.value = null
        data.value = null

        load()
    }

    open fun validate() {
        if (isLoginRequired) validators.validateLogin()
        if (isAgeConfirmationRequired) validators.validateAgeConfirmation()
    }
}
