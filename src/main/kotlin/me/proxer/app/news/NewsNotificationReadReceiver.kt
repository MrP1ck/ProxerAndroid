package me.proxer.app.news

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.launchAsync
import me.proxer.library.ProxerApi
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant

/**
 * @author Ruben Gees
 */
class NewsNotificationReadReceiver : BroadcastReceiver(), KoinComponent {

    companion object : KoinComponent {
        fun getPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, NewsNotificationReadReceiver::class.java)
                .addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val api by inject<ProxerApi>()
    private val preferenceHelper by inject<PreferenceHelper>()

    override fun onReceive(context: Context, intent: Intent?) {
        launchAsync {
            NewsNotifications.cancel(context)

            preferenceHelper.lastNewsDate = Instant.now()

            api.notifications.news()
                .markAsRead(true)
                .limit(0)
                .build()
                .awaitNullable()
        }
    }
}
