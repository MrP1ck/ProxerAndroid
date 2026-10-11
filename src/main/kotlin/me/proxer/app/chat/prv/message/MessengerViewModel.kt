package me.proxer.app.chat.prv.message

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.base.PagedViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerErrorEvent
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.extension.catchingErrors

/**
 * @author Ruben Gees
 */
class MessengerViewModel(
    dependencies: ViewModelDependencies,
    private val messengerDao: MessengerDao,
    initialConference: LocalConference
) : PagedViewModel<LocalMessage>(dependencies) {

    override val itemsOnPage = MessengerWorker.MESSAGES_ON_PAGE

    @Suppress("UNUSED_PARAMETER")
    override var hasReachedEnd
        get() = safeConference.isFullyLoaded
        set(value) = Unit

    val conference = MutableStateFlow(initialConference)
    val draft = Events<String>()
    val deleted = MutableStateFlow<Unit?>(null)

    /**
     * Stays loading until the synchronization delivered the messages, which arrive through the database.
     */
    override suspend fun loadData(): List<LocalMessage> {
        validators.validateLogin()

        when (page) {
            0 -> messengerDao.markConferenceAsRead(safeConference.id)
            else -> if (!hasReachedEnd) MessengerWorker.enqueueMessageLoad(safeConference.id)
        }

        awaitCancellation()
    }

    private val safeConference: LocalConference
        get() = conference.value

    private var draftJob: Job? = null

    init {
        viewModelScope.launch {
            messengerDao.getMessagesFlowForConference(initialConference.id).collect { messages ->
                if (storageHelper.isLoggedIn) {
                    if (messages.isEmpty()) {
                        if (!hasReachedEnd) {
                            MessengerWorker.enqueueMessageLoad(safeConference.id)
                        }
                    } else if (error.value == null) {
                        dataJob?.cancel()

                        page = messages.size / itemsOnPage
                        isLoading.value = false
                        error.value = null
                        data.value = messages

                        catchingErrors { messengerDao.markConferenceAsRead(safeConference.id) }
                    }
                }
            }
        }

        viewModelScope.launch {
            messengerDao.getConferenceFlow(initialConference.id).collect {
                if (it != null) {
                    conference.value = it
                } else {
                    deleted.value = Unit
                }
            }
        }

        viewModelScope.launch {
            bus.events<MessengerErrorEvent>().collect { event ->
                if (event.error is ChatMessageException) {
                    dataJob?.cancel()
                    isLoading.value = false
                    error.value = ErrorUtils.handle(event.error)
                }
            }
        }
    }

    fun loadDraft() {
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            withContext(Dispatchers.IO) { storageHelper.getMessageDraft(safeConference.id.toString()) }
                ?.let { draft.send(it) }
        }
    }

    fun updateDraft(draft: String) {
        draftJob?.cancel()
        draftJob = viewModelScope.launch(Dispatchers.IO) {
            if (draft.isBlank()) {
                storageHelper.deleteMessageDraft(safeConference.id.toString())
            } else {
                storageHelper.putMessageDraft(safeConference.id.toString(), draft)
            }
        }
    }

    fun sendMessage(text: String) {
        val safeUser = requireNotNull(storageHelper.user)

        viewModelScope.launch {
            catchingErrors {
                messengerDao.insertMessageToSend(safeUser, text, safeConference.id)

                if (!MessengerWorker.isRunning()) MessengerWorker.enqueueSynchronization()
            }
        }
    }
}
