package me.proxer.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.util.extension.await
import me.proxer.library.ProxerApi
import okhttp3.HttpUrl
import timber.log.Timber

class LinkCheckViewModel(private val api: ProxerApi) : ViewModel() {

    val data = MutableStateFlow<Boolean?>(null)
    val isLoading = MutableStateFlow(false)

    private var checkJob: Job? = null

    fun check(link: HttpUrl) {
        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            isLoading.value = true
            data.value = null

            try {
                data.value = api.messenger.checkLink(link).await().isSecure
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") checkError: Throwable) {
                Timber.e(checkError)

                data.value = false
            } finally {
                if (isActive) isLoading.value = false
            }
        }
    }
}
