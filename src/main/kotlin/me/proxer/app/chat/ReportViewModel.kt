package me.proxer.app.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.util.ErrorUtils
import timber.log.Timber

/**
 * @author Ruben Gees
 */
abstract class ReportViewModel : ViewModel() {

    val data = MutableStateFlow<Unit?>(null)
    val error = MutableStateFlow<ErrorUtils.ErrorAction?>(null)
    val isLoading = MutableStateFlow(false)

    private var dataJob: Job? = null

    fun sendReport(messageId: String, message: String) {
        if (!isLoading.value) {
            dataJob?.cancel()
            dataJob = viewModelScope.launch {
                error.value = null
                isLoading.value = true

                try {
                    report(messageId, message)

                    data.value = Unit
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") reportError: Throwable) {
                    Timber.e(reportError)

                    error.value = ErrorUtils.handle(reportError)
                } finally {
                    if (isActive) isLoading.value = false
                }
            }
        }
    }

    protected abstract suspend fun report(id: String, message: String)
}
