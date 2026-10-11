package me.proxer.app.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.toLocalSettings
import me.proxer.library.ProxerApi

/**
 * @author Ruben Gees
 */
class ProfileSettingsViewModel(
    private val api: ProxerApi,
    private val storageHelper: StorageHelper
) : ViewModel() {

    val data = MutableStateFlow(storageHelper.profileSettings)
    val error = Events<ErrorUtils.ErrorAction>()
    val updateError = Events<ErrorUtils.ErrorAction>()

    private var job: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        job?.cancel()
        job = viewModelScope.launch {
            catchingErrors {
                api.ucp.settings().await().toLocalSettings().also { storageHelper.profileSettings = it }
            }
                .onSuccess { data.value = it }
                .onFailure { error.send(ErrorUtils.handle(it)) }
        }
    }

    fun update(newData: LocalProfileSettings) {
        data.value = newData

        job?.cancel()
        job = viewModelScope.launch {
            catchingErrors {
                api.ucp.setSettings(newData.toNonLocalSettings()).awaitNullable()

                storageHelper.profileSettings = newData
            }.onFailure { updateError.send(ErrorUtils.handle(it)) }
        }
    }

    fun retryUpdate() {
        update(data.value)
    }
}
