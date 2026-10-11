package me.proxer.app.chat.prv.sync

import android.content.Context
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.LocalMessage
import me.proxer.app.chat.prv.conference.ConferenceFragmentPingEvent
import me.proxer.app.chat.prv.message.MessengerFragmentPingEvent
import me.proxer.app.exception.ChatException
import me.proxer.app.exception.ChatMessageException
import me.proxer.app.exception.ChatSendMessageException
import me.proxer.app.exception.ChatSynchronizationException
import me.proxer.app.util.WorkerUtils
import me.proxer.app.util.data.EventBus
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.toLocalConference
import me.proxer.app.util.extension.toLocalMessage
import me.proxer.library.ProxerApi
import me.proxer.library.ProxerException
import me.proxer.library.entity.messenger.Conference
import me.proxer.library.entity.messenger.Message
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.LinkedHashSet
import java.util.concurrent.TimeUnit

/**
 * @author Ruben Gees
 */
class MessengerWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams), KoinComponent {

    companion object : KoinComponent {
        const val CONFERENCES_ON_PAGE = 48
        const val MESSAGES_ON_PAGE = 30

        private const val NAME = "MessengerWorker"
        private const val CONFERENCE_ID_ARGUMENT = "conference_id"

        suspend fun isRunning() = withContext(Dispatchers.IO) {
            workManager.getWorkInfosForUniqueWork(NAME).get().all { info -> info.state == WorkInfo.State.RUNNING }
        }

        private val bus by inject<EventBus>()
        private val workManager by inject<WorkManager>()
        private val storageHelper by inject<StorageHelper>()
        private val preferenceHelper by inject<PreferenceHelper>()

        fun enqueueSynchronizationIfPossible() = when {
            canSchedule() -> enqueueSynchronization()
            else -> cancel()
        }

        fun enqueueSynchronization() = doEnqueue()

        fun enqueueMessageLoad(conferenceId: Long) = doEnqueue(conferenceId = conferenceId)

        fun cancel() {
            workManager.cancelUniqueWork(NAME)
        }

        private fun reschedule(synchronizationResult: SynchronizationResult) {
            if (canSchedule() && synchronizationResult != SynchronizationResult.ERROR) {
                if (synchronizationResult == SynchronizationResult.CHANGES || bus.post(MessengerFragmentPingEvent())) {
                    storageHelper.resetChatInterval()

                    doEnqueue(3_000L)
                } else if (bus.post(ConferenceFragmentPingEvent())) {
                    storageHelper.resetChatInterval()

                    doEnqueue(10_000L)
                } else {
                    storageHelper.incrementChatInterval()

                    doEnqueue(storageHelper.chatInterval)
                }
            }
        }

        private fun doEnqueue(startTime: Long? = null, conferenceId: Long? = null) {
            val workRequest = OneTimeWorkRequestBuilder<MessengerWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .apply { if (startTime != null) setInitialDelay(startTime, TimeUnit.MILLISECONDS) }
                .setInputData(
                    Data.Builder()
                        .apply { if (conferenceId != null) putLong(CONFERENCE_ID_ARGUMENT, conferenceId) }
                        .build()
                )
                .build()

            workManager.beginUniqueWork(NAME, ExistingWorkPolicy.REPLACE, workRequest).enqueue()
        }

        private fun canSchedule() = preferenceHelper.areChatNotificationsEnabled ||
            bus.post(ConferenceFragmentPingEvent()) || bus.post(MessengerFragmentPingEvent())

        private fun canShowNotification() = preferenceHelper.areChatNotificationsEnabled &&
            !bus.post(ConferenceFragmentPingEvent()) && !bus.post(MessengerFragmentPingEvent())
    }

    private val conferenceId: Long
        get() = inputData.getLong(CONFERENCE_ID_ARGUMENT, 0L)

    private val api by inject<ProxerApi>()
    private val storageHelper by inject<StorageHelper>()
    private val messengerDatabase by inject<MessengerDatabase>()
    private val messengerDao by inject<MessengerDao>()

    override suspend fun doWork(): Result {
        if (!storageHelper.isLoggedIn) return Result.failure()

        val synchronizationResult = when (conferenceId) {
            0L ->
                try {
                    handleSynchronization()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
                    handleSynchronizationError(error)
                }
            else ->
                try {
                    handleLoadMoreMessages(conferenceId)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
                    handleLoadMoreMessagesError(error)
                }
        }

        reschedule(synchronizationResult)

        return if (synchronizationResult != SynchronizationResult.ERROR) Result.success() else Result.failure()
    }

    private suspend fun handleSynchronization(): SynchronizationResult {
        val newConferencesAndMessages = synchronize()

        storageHelper.areConferencesSynchronized = true

        val maxNewDate = newConferencesAndMessages.flatMap { it.value }.maxByOrNull { it.date }?.date

        if (!isStopped && newConferencesAndMessages.isNotEmpty()) {
            bus.post(SynchronizationEvent())
        }

        if (!isStopped && maxNewDate != null && maxNewDate > storageHelper.lastChatMessageDate) {
            if (canShowNotification()) {
                showNotification(applicationContext)
            }

            storageHelper.lastChatMessageDate = maxNewDate
        }

        MessengerShortcuts.updateShareTargets(applicationContext)

        return when (newConferencesAndMessages.isNotEmpty()) {
            true -> SynchronizationResult.CHANGES
            false -> SynchronizationResult.NO_CHANGES
        }
    }

    private fun handleSynchronizationError(error: Throwable): SynchronizationResult {
        Timber.e(error)

        if (!isStopped) {
            when (error) {
                is ChatException -> bus.post(MessengerErrorEvent(error))
                else -> bus.post(MessengerErrorEvent(ChatSynchronizationException(error)))
            }
        }

        return when (WorkerUtils.shouldRetryForError(error)) {
            true -> SynchronizationResult.NO_CHANGES
            false -> SynchronizationResult.ERROR
        }
    }

    private fun handleLoadMoreMessagesError(error: Throwable): SynchronizationResult {
        Timber.e(error)

        if (!isStopped) {
            when (error) {
                is ChatException -> bus.post(MessengerErrorEvent(error))
                else -> bus.post(MessengerErrorEvent(ChatMessageException(error)))
            }
        }

        return when (WorkerUtils.shouldRetryForError(error)) {
            true -> SynchronizationResult.NO_CHANGES
            false -> SynchronizationResult.ERROR
        }
    }

    private suspend fun handleLoadMoreMessages(conferenceId: Long): SynchronizationResult {
        val fetchedMessages = loadMoreMessages(conferenceId)

        fetchedMessages.maxByOrNull { it.date }?.date?.toInstant()?.let { mostRecentDate ->
            if (mostRecentDate.isAfter(storageHelper.lastChatMessageDate)) {
                storageHelper.lastChatMessageDate = mostRecentDate
            }
        }

        return SynchronizationResult.CHANGES
    }

    @Suppress("RethrowCaughtException")
    private suspend fun synchronize(): Map<LocalConference, List<LocalMessage>> {
        val sentMessages = sendMessages()

        val newConferencesAndMessages = try {
            markConferencesAsRead(
                messengerDao.getConferencesToMarkAsRead()
                    .asSequence()
                    .plus(sentMessages.map { messengerDao.findConference(it.conferenceId) })
                    .distinct()
                    .filterNotNull()
                    .toList()
            )

            fetchConferences().associate { conference ->
                fetchNewMessages(conference).let { (messages, isFullyLoaded) ->
                    val isLocallyFullyLoaded = messengerDao.findConference(conference.id.toLong())
                        ?.isFullyLoaded == true

                    conference.toLocalConference(isLocallyFullyLoaded || isFullyLoaded) to
                        messages.map { it.toLocalMessage() }.asReversed()
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
            withContext(NonCancellable) {
                messengerDatabase.withTransaction {
                    sentMessages.forEach {
                        messengerDao.deleteMessageToSend(it.id)
                    }
                }
            }

            throw error
        }

        messengerDatabase.withTransaction {
            newConferencesAndMessages.let {
                messengerDao.insertConferences(it.keys.toList())
                messengerDao.insertMessages(it.values.flatten())
            }

            sentMessages.forEach {
                messengerDao.deleteMessageToSend(it.id)
            }
        }

        return newConferencesAndMessages
    }

    private suspend fun loadMoreMessages(conferenceId: Long): List<Message> {
        val fetchedMessages = fetchMoreMessages(conferenceId)

        messengerDatabase.withTransaction {
            messengerDao.insertMessages(fetchedMessages.map { it.toLocalMessage() })

            if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                messengerDao.markConferenceAsFullyLoaded(conferenceId)
            }
        }

        return fetchedMessages
    }

    @Suppress("RethrowCaughtException")
    private suspend fun sendMessages() = messengerDao.getMessagesToSend().apply {
        forEachIndexed { index, (messageId, conferenceId, _, _, message) ->
            val result = try {
                api.messenger.sendMessage(conferenceId.toString(), message).awaitNullable()
            } catch (cancellation: CancellationException) {
                // The message was most likely not sent, but the previous ones are. Delete them to avoid resending.
                withContext(NonCancellable) {
                    messengerDatabase.withTransaction {
                        for (i in 0 until index) {
                            messengerDao.deleteMessageToSend(get(i).id)
                        }
                    }
                }

                throw cancellation
            } catch (error: ProxerException) {
                if (error.cause?.stackTrace?.find { it.methodName.contains("read") } != null) {
                    Timber.w("Error while sending message. Removing it to avoid sending duplicate.")

                    // The message was sent, but we did not receive a proper api answer due to slow network, return
                    // non-null to handle it like the non-empty result case.
                    "error"
                } else {
                    // The message was most likely not sent, but the previous ones are. Delete them to avoid resending.
                    messengerDatabase.withTransaction {
                        for (i in 0 until index) {
                            messengerDao.deleteMessageToSend(get(i).id)
                        }
                    }

                    throw error
                }
            }

            // Per documentation: The api may return some String in case something went wrong.
            if (result != null) {
                // Delete all messages we have correctly sent already.
                messengerDatabase.withTransaction {
                    for (i in 0..index) {
                        messengerDao.deleteMessageToSend(get(i).id)
                    }
                }

                throw ChatSendMessageException(
                    ProxerException(
                        ProxerException.ErrorType.SERVER,
                        ProxerException.ServerErrorType.MESSAGES_INVALID_MESSAGE,
                        result
                    ),
                    messageId
                )
            }
        }
    }

    private suspend fun markConferencesAsRead(conferencesToMarkAsRead: List<LocalConference>) {
        conferencesToMarkAsRead.forEach {
            api.messenger.markConferenceAsRead(it.id.toString()).awaitNullable()
        }
    }

    private suspend fun fetchConferences(): Collection<Conference> {
        val changedConferences = LinkedHashSet<Conference>()
        var page = 0

        while (true) {
            val fetchedConferences = api.messenger.conferences()
                .page(page)
                .await()

            changedConferences += fetchedConferences.filter {
                it != messengerDao.findConference(it.id.toLong())?.toNonLocalConference()
            }

            if (changedConferences.size / (page + 1) < CONFERENCES_ON_PAGE) {
                break
            } else {
                page++
            }
        }

        return changedConferences
    }

    private suspend fun fetchNewMessages(conference: Conference): Pair<List<Message>, Boolean> {
        val mostRecentLocalMessage = messengerDao.findMostRecentMessageForConference(conference.id.toLong())

        return when (val mostRecentMessage = mostRecentLocalMessage?.toNonLocalMessage()) {
            null -> fetchForEmptyConference(conference)
            else -> fetchForExistingConference(conference, mostRecentMessage)
        }
    }

    private suspend fun fetchForEmptyConference(conference: Conference): Pair<List<Message>, Boolean> {
        val newMessages = mutableListOf<Message>()

        var unreadAmount = 0
        var nextId = "0"

        while (unreadAmount < conference.unreadMessageAmount) {
            val fetchedMessages = api.messenger.messages()
                .conferenceId(conference.id)
                .messageId(nextId)
                .markAsRead(false)
                .await()

            newMessages += fetchedMessages

            if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                return newMessages to true
            } else {
                unreadAmount += fetchedMessages.size
                nextId = fetchedMessages.first().id
            }
        }

        return newMessages to false
    }

    private suspend fun fetchForExistingConference(
        conference: Conference,
        mostRecentMessage: Message
    ): Pair<List<Message>, Boolean> {
        val mostRecentMessageIdBeforeUpdate = mostRecentMessage.id.toLong()
        val newMessages = mutableListOf<Message>()

        var existingUnreadMessageAmount = messengerDao.getUnreadMessageAmountForConference(
            conference.id.toLong(),
            conference.lastReadMessageId.toLong()
        )

        var currentMessage: Message = mostRecentMessage
        var nextId = "0"

        while (currentMessage.date < conference.date || existingUnreadMessageAmount < conference.unreadMessageAmount) {
            val fetchedMessages = api.messenger.messages()
                .conferenceId(conference.id)
                .messageId(nextId)
                .markAsRead(false)
                .await()

            newMessages += fetchedMessages

            if (fetchedMessages.size < MESSAGES_ON_PAGE) {
                return newMessages.filter { it.id.toLong() > mostRecentMessageIdBeforeUpdate } to true
            } else {
                existingUnreadMessageAmount += fetchedMessages.size
                currentMessage = fetchedMessages.last()
                nextId = fetchedMessages.first().id
            }
        }

        return newMessages.filter { it.id.toLong() > mostRecentMessageIdBeforeUpdate } to false
    }

    private suspend fun fetchMoreMessages(conferenceId: Long) = api.messenger.messages()
        .conferenceId(conferenceId.toString())
        .messageId(messengerDao.findOldestMessageForConference(conferenceId)?.id?.toString() ?: "0")
        .markAsRead(false)
        .await()
        .asReversed()

    private suspend fun showNotification(context: Context) {
        val unreadMap = messengerDao.getUnreadConferences().associateWith {
            messengerDao
                .getMostRecentMessagesForConference(it.id, it.unreadMessageAmount)
                .asReversed()
        }

        MessengerNotifications.showOrUpdate(context, unreadMap)
    }

    class SynchronizationEvent

    private enum class SynchronizationResult {
        CHANGES, NO_CHANGES, ERROR
    }
}
