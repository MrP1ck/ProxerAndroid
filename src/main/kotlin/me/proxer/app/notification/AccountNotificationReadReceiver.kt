package me.proxer.app.notification

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.launchAsync
import me.proxer.library.ProxerApi
import me.proxer.library.enums.NotificationFilter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Ruben Gees
 */
class AccountNotificationReadReceiver : BroadcastReceiver(), KoinComponent {

    companion object : KoinComponent {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, AccountNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val api by inject<ProxerApi>()

    override fun onReceive(context: Context, intent: Intent?) {
        launchAsync {
            AccountNotifications.cancel(context)

            api.notifications.notifications()
                .limit(Int.MAX_VALUE)
                .markAsRead(true)
                .filter(NotificationFilter.UNREAD)
                .build()
                .awaitNullable()
        }
    }
}
