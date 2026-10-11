package me.proxer.app.chat.pub.message

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.retryWithDelay
import me.proxer.app.util.extension.toParsedMessage
import me.proxer.library.enums.ChatMessageAction
import org.threeten.bp.Instant
import timber.log.Timber
import java.util.Collections.emptyList
import java.util.LinkedList
import java.util.Queue

/**
 * @author Ruben Gees
 */
class ChatViewModel(private val chatRoomId: String) : PagedViewModel<ParsedChatMessage>() {

    private companion object {
        private const val POLLING_INTERVAL = 3_000L
        private const val SEND_ATTEMPTS = 2
    }

    override val itemsOnPage = 50

    override suspend fun loadData(): List<ParsedChatMessage> {
        validate()

        return loadMessages(data.value?.lastOrNull()?.id ?: "0")
    }

    val sendMessageError = Events<ErrorUtils.ErrorAction>()
    val draft = Events<String>()

    private var pollingJob: Job? = null
    private var draftJob: Job? = null

    private val sendMessageQueue: Queue<ParsedChatMessage> = LinkedList()
    private val sentMessageIds = mutableSetOf<String>()
    private var sendMessageJob: Job? = null

    private var currentFirstId = "0"

    override fun load() {
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            error.value = null
            isLoading.value = true

            try {
                val newData = loadData()

                hasReachedEnd = newData.size < itemsOnPage

                val mergedData = mergeNewDataWithExistingData(newData, data.value?.lastOrNull()?.id ?: "0")

                isRefreshing = false

                if (pollingJob == null) startPolling()

                currentFirstId = findFirstRemoteId(mergedData) ?: "0"
                error.value = null
                data.value = mergedData
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (@Suppress("TooGenericExceptionCaught") loadError: Throwable) {
                Timber.e(loadError)

                isRefreshing = false

                if ((data.value?.size ?: 0) > 0) {
                    refreshError.send(ErrorUtils.handle(loadError))
                } else {
                    error.value = ErrorUtils.handle(loadError)
                }
            } finally {
                if (isActive) isLoading.value = false
            }
        }
    }

    fun loadDraft() {
        draftJob?.cancel()
        draftJob = viewModelScope.launch {
            withContext(Dispatchers.IO) { storageHelper.getMessageDraft(chatRoomId) }?.let { draft.send(it) }
        }
    }

    fun updateDraft(draft: String) {
        draftJob?.cancel()
        draftJob = viewModelScope.launch(Dispatchers.IO) {
            if (draft.isBlank()) {
                storageHelper.deleteMessageDraft(chatRoomId)
            } else {
                storageHelper.putMessageDraft(chatRoomId, draft)
            }
        }
    }

    fun sendMessage(text: String) {
        storageHelper.user?.let { user ->
            val firstId = data.value?.firstOrNull()?.id?.toLong()
            val nextId = if (firstId == null || firstId >= 0) -1 else firstId - 1

            val message = ParsedChatMessage(
                nextId.toString(),
                user.id,
                user.name,
                user.image,
                text,
                ChatMessageAction.NONE,
                Instant.now()
            )

            data.value = listOf(message).plus(data.value ?: emptyList())
            sendMessageQueue.offer(message)

            if (sendMessageJob?.isActive != true) {
                doSendMessages()
            }
        }
    }

    fun pausePolling() {
        pollingJob?.cancel()
    }

    fun resumePolling() {
        if (data.value != null) {
            startPolling(true)
        }
    }

    private suspend fun loadMessages(firstId: String) = withContext(Dispatchers.Default) {
        api.chat.messages(chatRoomId)
            .messageId(firstId)
            .await()
            .map { message -> message.toParsedMessage() }
    }

    private fun mergeNewDataWithExistingData(
        newData: List<ParsedChatMessage>,
        currentId: String
    ): List<ParsedChatMessage> {
        val messageIdsToDelete = newData
            .filter { it.action == ChatMessageAction.REMOVE_MESSAGE }
            .flatMap { listOf(it.id, it.message) }

        val previousSentMessageIdAmount = sentMessageIds.size
        val existingUnsendMessages = data.value?.takeWhile { it.id.toLong() < 0 } ?: emptyList()

        sentMessageIds.removeAll(newData.map { it.id })

        val result = data.value.let { existingData ->
            when (existingData) {
                null -> newData
                else -> when (currentId) {
                    "0" ->
                        newData + existingData
                            .asSequence()
                            .dropWhile { it.id.toLong() < 0 }
                            .filter { oldItem -> newData.none { newItem -> oldItem.id == newItem.id } }
                            .toList()
                    else ->
                        existingData
                            .asSequence()
                            .dropWhile { it.id.toLong() < 0 }
                            .filter { oldItem -> newData.none { newItem -> oldItem.id == newItem.id } }
                            .toList() + newData
                }
            }
        }

        val mergedResult = existingUnsendMessages.dropLast(previousSentMessageIdAmount - sentMessageIds.size) + result

        return when (messageIdsToDelete.isNotEmpty()) {
            true -> mergedResult.filterNot { it.id in messageIdsToDelete }
            false -> mergedResult
        }
    }

    private fun startPolling(immediate: Boolean = false) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            if (!immediate) delay(POLLING_INTERVAL)

            while (true) {
                try {
                    validators.validateLogin()

                    val mergedData = mergeNewDataWithExistingData(loadMessages("0"), "0")
                    val firstId = findFirstRemoteId(mergedData)

                    if (firstId != null && firstId != currentFirstId) {
                        currentFirstId = firstId
                        data.value = mergedData
                    }
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") pollingError: Throwable) {
                    // Try again with the next poll.
                    Timber.w(pollingError)
                }

                delay(POLLING_INTERVAL)
            }
        }
    }

    private fun doSendMessages() {
        sendMessageJob = viewModelScope.launch {
            while (true) {
                val item = sendMessageQueue.poll() ?: break

                val result = catchingErrors {
                    retryWithDelay(SEND_ATTEMPTS, POLLING_INTERVAL) {
                        validators.validateLogin()

                        api.chat.sendMessage(chatRoomId, item.message).await()
                    }
                }

                result.onSuccess { id ->
                    sentMessageIds.add(id)
                    startPolling(true)
                }

                result.onFailure {
                    data.value = data.value?.dropWhile { message -> message.id.toLong() < 0 }
                    sendMessageQueue.clear()
                    sendMessageError.send(ErrorUtils.handle(it))
                }
            }
        }
    }

    private fun findFirstRemoteId(data: List<ParsedChatMessage>): String? {
        return data.dropWhile { it.id.toLong() < 0 }.firstOrNull()?.id
    }
}
