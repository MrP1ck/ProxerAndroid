package me.proxer.app.auth

import android.content.Context
import androidx.core.content.pm.ShortcutManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.AccountNotifications
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.catchingErrors

/**
 * @author Ruben Gees
 */
class LoginHandler(
    private val storageHelper: StorageHelper,
    private val preferenceHelper: PreferenceHelper,
    private val messengerDao: MessengerDao
) {

    fun listen(scope: CoroutineScope, context: Context) {
        scope.launch {
            storageHelper.isLoggedInChanges.collect { isLoggedIn ->
                if (isLoggedIn) {
                    onLogin()
                } else {
                    onLogout(context)
                }
            }
        }
    }

    private fun onLogin() {
        MessengerWorker.enqueueSynchronizationIfPossible()
        NotificationWorker.enqueueIfPossible()
    }

    private suspend fun onLogout(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)

        AccountNotifications.cancel(context)
        MessengerNotifications.cancel(context)

        MessengerWorker.cancel()

        if (!preferenceHelper.areNewsNotificationsEnabled) {
            NotificationWorker.cancel()
        }

        catchingErrors {
            withContext(Dispatchers.IO) {
                storageHelper.reset()
                messengerDao.clear()
            }
        }
    }
}
