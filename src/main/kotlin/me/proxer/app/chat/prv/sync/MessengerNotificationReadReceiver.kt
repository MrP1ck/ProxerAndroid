package me.proxer.app.chat.prv.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.proxer.app.util.extension.launchAsync
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * @author Ruben Gees
 */
class MessengerNotificationReadReceiver : BroadcastReceiver(), KoinComponent {

    companion object : KoinComponent {
        private const val CONFERENCE_ID_EXTRA = "conference_id"

        fun getPendingIntent(context: Context, conferenceId: Long): PendingIntent {
            val intent = Intent(context, MessengerNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                .apply { putExtra(CONFERENCE_ID_EXTRA, conferenceId) }

            return PendingIntent.getBroadcast(
                context,
                conferenceId.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private val messengerDao by inject<MessengerDao>()

    override fun onReceive(context: Context, intent: Intent) {
        val conferenceId = intent.getLongExtra(CONFERENCE_ID_EXTRA, -1L)

        launchAsync {
            messengerDao.markConferenceAsRead(conferenceId)

            val unreadMap = messengerDao.getUnreadConferences()
                .associateWith {
                    messengerDao.getMostRecentMessagesForConference(it.id, it.unreadMessageAmount).asReversed()
                }
                .plus(messengerDao.getConference(conferenceId) to emptyList())

            MessengerNotifications.showOrUpdate(context, unreadMap)
        }
    }
}
