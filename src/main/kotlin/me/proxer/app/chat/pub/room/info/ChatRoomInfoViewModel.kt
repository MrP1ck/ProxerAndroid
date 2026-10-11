package me.proxer.app.chat.pub.room.info

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseContentViewModel
import me.proxer.library.api.Endpoint
import me.proxer.library.entity.chat.ChatRoomUser
import timber.log.Timber

/**
 * @author Ruben Gees
 */
class ChatRoomInfoViewModel(private val chatRoomId: String) : BaseContentViewModel<List<ChatRoomUser>>() {

    private companion object {
        private const val POLLING_INTERVAL = 10_000L
    }

    override val endpoint: Endpoint<List<ChatRoomUser>>
        get() = api.chat.roomUsers(chatRoomId)

    private var pollingJob: Job? = null

    override suspend fun loadData(): List<ChatRoomUser> = loadSortedUsers().also {
        if (pollingJob == null) startPolling()
    }

    fun pausePolling() {
        pollingJob?.cancel()
    }

    fun resumePolling() {
        if (data.value != null) {
            startPolling()
        }
    }

    private suspend fun loadSortedUsers() = super.loadData()
        .sortedWith(compareByDescending(ChatRoomUser::isModerator).thenBy(ChatRoomUser::name))

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(POLLING_INTERVAL)

                try {
                    val users = loadSortedUsers()

                    error.value = null
                    data.value = users
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (@Suppress("TooGenericExceptionCaught") pollingError: Throwable) {
                    // Try again with the next poll.
                    Timber.w(pollingError)
                }
            }
        }
    }
}
