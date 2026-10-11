package me.proxer.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.library.ProxerApi
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ServerErrorType
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class LoginViewModel : ViewModel() {

    val success = MutableStateFlow<Unit?>(null)
    val error = MutableStateFlow<ErrorUtils.ErrorAction?>(null)
    val isLoading = MutableStateFlow(false)
    val isTwoFactorAuthenticationEnabled = MutableStateFlow<Boolean?>(null)

    private val api by safeInject<ProxerApi>()
    private val storageHelper by safeInject<StorageHelper>()
    private val preferenceHelper by safeInject<PreferenceHelper>()

    private var dataJob: Job? = null

    init {
        isTwoFactorAuthenticationEnabled.value = preferenceHelper.isTwoFactorAuthenticationEnabled
    }

    fun login(username: String, password: String, secretKey: String?) {
        if (!isLoading.value) {
            dataJob?.cancel()
            dataJob = viewModelScope.launch {
                error.value = null
                isLoading.value = true

                try {
                    performLogin(username, password, secretKey)

                    success.value = Unit
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (loginError: ProxerException) {
                    if (loginError.serverErrorType == ServerErrorType.USER_2FA_SECRET_REQUIRED) {
                        preferenceHelper.isTwoFactorAuthenticationEnabled = true
                        isTwoFactorAuthenticationEnabled.value = true
                    }

                    handleLoginError(loginError)
                } catch (@Suppress("TooGenericExceptionCaught") loginError: Throwable) {
                    handleLoginError(loginError)
                } finally {
                    if (isActive) isLoading.value = false
                }
            }
        }
    }

    private fun handleLoginError(loginError: Throwable) {
        Timber.e(loginError)

        error.value = ErrorUtils.handle(loginError)
    }

    private suspend fun performLogin(username: String, password: String, secretKey: String?) {
        try {
            val user = api.user.login(username, password).secretKey(secretKey).await()

            storageHelper.temporaryToken = user.loginToken

            val settings = api.ucp.settings().await()

            storageHelper.user = LocalUser(user.loginToken, user.id, username, user.image)
            storageHelper.profileSettings = settings.toLocalSettings()
        } finally {
            withContext(NonCancellable) { storageHelper.temporaryToken = null }
        }
    }
}
