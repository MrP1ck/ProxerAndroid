package me.proxer.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import me.proxer.app.TestApplication
import me.proxer.app.util.data.ResettingMutableLiveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * One-shot events of a [ResettingMutableLiveData] reset themselves right after they are delivered, which
 * observeAsState misses. [LiveDataEffect] must still receive every event.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class LiveDataEffectTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun receivesEveryEventOfResettingLiveData() {
        val liveData = ResettingMutableLiveData<String?>()
        val received = mutableListOf<String>()

        composeRule.setContent {
            LiveDataEffect(liveData) { received += it }
        }

        composeRule.runOnIdle { liveData.value = "first" }
        composeRule.runOnIdle { liveData.value = "second" }
        composeRule.waitForIdle()

        assertEquals(listOf("first", "second"), received)
        assertNull("The event should be reset after it was delivered", liveData.value)
    }
}
