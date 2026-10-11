package me.proxer.app.notification

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.proxer.app.base.PagedViewModel
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.UniqueQueue
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.toInstantBP
import me.proxer.library.enums.NotificationFilter

/**
 * @author Ruben Gees
 */
class NotificationViewModel : PagedViewModel<ProxerNotification>() {

    override val itemsOnPage = 30

    override suspend fun loadData(): List<ProxerNotification> {
        validators.validateLogin()

        val notifications = when (page) {
            0 -> {
                val unreadResult = api.notifications.notifications()
                    .markAsRead(true)
                    .page(page)
                    .filter(NotificationFilter.UNREAD)
                    .limit(Int.MAX_VALUE)
                    .await()

                firstPageItemAmount = unreadResult.size

                mergeNewDataWithExistingData(unreadResult, loadReadNotifications(), 0)
            }
            else -> loadReadNotifications()
        }

        if (page == 0) {
            notifications.firstOrNull()?.date?.toInstantBP()?.let { date ->
                storageHelper.lastNotificationsDate = date
            }
        }

        return notifications
    }

    val deletionError = Events<ErrorUtils.ErrorAction>()

    private val deletionQueue = UniqueQueue<ProxerNotification>()
    private var deletionJob: Job? = null
    private var deletionAllJob: Job? = null

    private var firstPageItemAmount = 0

    init {
        // Being registered tells the notification worker that the notifications are visible.
        viewModelScope.launch { bus.events<AccountNotificationEvent>().collect { } }
    }

    override fun refresh() = reload()

    override fun areItemsTheSame(old: ProxerNotification, new: ProxerNotification): Boolean {
        return old == new
    }

    fun addItemToDelete(item: ProxerNotification) {
        deletionQueue.add(item)

        if (deletionJob?.isActive != true) {
            startItemDeletion()
        }
    }

    fun deleteAll() {
        deletionAllJob?.cancel()
        deletionJob?.cancel()
        deletionQueue.clear()

        deletionAllJob = viewModelScope.launch {
            catchingErrors { api.notifications.deleteAllNotifications().awaitNullable() }
                .onSuccess { data.value = emptyList() }
                .onFailure { deletionError.send(ErrorUtils.handle(it)) }
        }
    }

    private fun startItemDeletion() {
        deletionAllJob?.cancel()

        deletionJob = viewModelScope.launch {
            while (true) {
                val item = deletionQueue.poll() ?: break

                val result = catchingErrors { api.notifications.deleteNotification(item.id).awaitNullable() }

                result.onSuccess { data.value = data.value?.filterNot { newItem -> newItem == item } }

                result.onFailure {
                    deletionQueue.clear()
                    deletionError.send(ErrorUtils.handle(it))
                }
            }
        }
    }

    private suspend fun loadReadNotifications() = api.notifications.notifications()
        .page(page - firstPageItemAmount / itemsOnPage)
        .filter(NotificationFilter.READ)
        .limit(itemsOnPage)
        .await()
}
