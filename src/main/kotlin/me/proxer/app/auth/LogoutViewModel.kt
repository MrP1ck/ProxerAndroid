package me.proxer.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class LogoutViewModel : ViewModel() {

    val success = MutableStateFlow<Unit?>(null)
    val error = MutableStateFlow<ErrorUtils.ErrorAction?>(null)
    val isLoading = MutableStateFlow(false)

    private val api by safeInject<ProxerApi>()

    private var dataJob: Job? = null

    fun logout() {
        if (!isLoading.value) {
            dataJob?.cancel()
            dataJob = viewModelScope.launch {
                error.value = null
                isLoading.value = true

                try {
                    api.user.logout().awaitNullable()

                    success.value = Unit
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") logoutError: Throwable) {
                    Timber.e(logoutError)

                    error.value = ErrorUtils.handle(logoutError)
                } finally {
                    if (isActive) isLoading.value = false
                }
            }
        }
    }
}
