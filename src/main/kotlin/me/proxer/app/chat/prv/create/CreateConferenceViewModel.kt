package me.proxer.app.chat.prv.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.EventBus
import me.proxer.app.util.data.Events
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.safeInject
import me.proxer.library.ProxerApi
import me.proxer.library.api.Endpoint

/**
 * @author Ruben Gees
 */
class CreateConferenceViewModel : ViewModel() {

    private val bus by safeInject<EventBus>()
    private val api by safeInject<ProxerApi>()
    private val messengerDao by safeInject<MessengerDao>()

    val isLoading = MutableStateFlow(false)
    val result = Events<LocalConference>()
    val error = Events<ErrorUtils.ErrorAction>()

    private var newConferenceId: Long? = null
    private var creationJob: Job? = null

    init {
        viewModelScope.launch {
            bus.events<MessengerWorker.SynchronizationEvent>().collect {
                val foundConference = newConferenceId?.let { id -> messengerDao.findConference(id) }

                if (foundConference != null) {
                    newConferenceId = null
                    isLoading.value = false
                    result.send(foundConference)
                }
            }
        }

        viewModelScope.launch {
            bus.events<MessengerErrorEvent>().collect {
                if (newConferenceId != null) {
                    newConferenceId = null
                    isLoading.value = false
                    error.send(ErrorUtils.handle(it.error))
                }
            }
        }
    }

    fun createGroup(topic: String, firstMessage: String, participants: List<Participant>) = createConference(
        api
            .messenger
            .createConferenceGroup(topic, firstMessage, participants.map { it.username })
    )

    fun createChat(firstMessage: String, participant: Participant) = createConference(
        api
            .messenger
            .createConference(firstMessage, participant.username)
    )

    private fun createConference(endpoint: Endpoint<String>) {
        creationJob?.cancel()
        creationJob = viewModelScope.launch {
            newConferenceId = null
            isLoading.value = true

            catchingErrors { endpoint.await() }
                .onSuccess {
                    newConferenceId = it.toLong()

                    MessengerWorker.enqueueSynchronization()
                }
                .onFailure {
                    isLoading.value = false
                    error.send(ErrorUtils.handle(it))
                }
        }
    }
}
