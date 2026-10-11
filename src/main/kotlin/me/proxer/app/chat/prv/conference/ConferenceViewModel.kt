package me.proxer.app.chat.prv.conference

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.extension.safeInject

/**
 * @author Ruben Gees
 */
class ConferenceViewModel(searchQuery: String) : BaseViewModel<List<ConferenceWithMessage>>() {

    private val messengerDao by safeInject<MessengerDao>()

    private val searchQueryFlow = MutableStateFlow(searchQuery)

    var searchQuery: String
        get() = searchQueryFlow.value
        set(value) {
            searchQueryFlow.value = value
        }

    /**
     * Stays loading until the synchronization delivered the first conferences, which arrive through the database.
     */
    override suspend fun loadData(): List<ConferenceWithMessage> {
        validate()

        if (!MessengerWorker.isRunning()) MessengerWorker.enqueueSynchronization()

        awaitCancellation()
    }

    init {
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            searchQueryFlow.flatMapLatest { messengerDao.getConferencesFlow(it) }.collect {
                val containsRelevantData = it.isNotEmpty() || storageHelper.areConferencesSynchronized

                if (containsRelevantData && storageHelper.isLoggedIn && error.value == null) {
                    dataJob?.cancel()
                    isLoading.value = false
                    error.value = null
                    data.value = it
                }
            }
        }

        viewModelScope.launch {
            bus.events<MessengerErrorEvent>().collect {
                if (isLoading.value) {
                    dataJob?.cancel()
                    isLoading.value = false
                    data.value = null
                    error.value = ErrorUtils.handle(it.error)
                }
            }
        }
    }
}
