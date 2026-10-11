package me.proxer.app.notification

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import me.proxer.app.TestApplication
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class AccountNotificationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun pagesOfTheAppAreOpenedInTheApp() {
        val intent = AccountNotifications.contentIntent(context, "https://proxer.me/info/53#top".toHttpUrl())

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(context.packageName, intent.`package`)
        assertEquals("https://proxer.me/info/53#top", intent.dataString)
    }

    @Test
    fun otherPagesShowTheListOfNotifications() {
        val intent = AccountNotifications.contentIntent(context, "https://proxer.me/team".toHttpUrl())

        assertEquals(NotificationActivity::class.java.name, intent.component?.className)
    }
}
