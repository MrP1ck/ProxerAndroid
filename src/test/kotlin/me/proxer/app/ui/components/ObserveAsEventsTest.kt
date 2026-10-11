package me.proxer.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import me.proxer.app.TestApplication
import me.proxer.app.util.data.Events
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ObserveAsEvents] must receive every event, even if several are sent before the UI got to handle them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ObserveAsEventsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun receivesEveryEvent() {
        val events = Events<String>()
        val received = mutableListOf<String>()

        composeRule.setContent {
            ObserveAsEvents(events) { received += it }
        }

        composeRule.runOnIdle {
            events.send("first")
            events.send("second")
        }
        composeRule.waitForIdle()

        assertEquals(listOf("first", "second"), received)
    }

    @Test
    fun deliversEventsSentBeforeTheScreenIsShown() {
        val events = Events<String>()
        val received = mutableListOf<String>()

        events.send("early")

        composeRule.setContent {
            ObserveAsEvents(events) { received += it }
        }
        composeRule.waitForIdle()

        assertEquals(listOf("early"), received)
    }
}
