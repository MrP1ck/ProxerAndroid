package me.proxer.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import me.proxer.app.TestApplication
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.compose.BBCodeContent
import me.proxer.app.ui.view.bbcode.toBBTree
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders a forum post using most BBCode tags with the Compose renderer.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestApplication::class, qualifiers = "w411dp-h1400dp-xxhdpi")
class BBCodeScreenshotTest(private val isDark: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun parameters() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))

        private val POST = """
            Hallo [b]zusammen[/b], das ist [i]kursiv[/i], [u]unterstrichen[/u] und [s]durchgestrichen[/s].
            [color=#E53935]Roter Text[/color] und [size=5]großer Text[/size] mit einem [url=https://proxer.me]Link[/url].
            Erwähnung von @Nutzer und https://proxer.me/info/53

            [quote=Nutzer]Das ist ein Zitat mit [b]fettem[/b] Text.[/quote]

            [spoiler=Handlung]Das sollte versteckt sein.[/spoiler]

            [code]val answer = 42[/code]

            [ul][li]Erster Punkt[/li][li]Zweiter Punkt[/li][/ul]
            [ol][li]Eins[/li][li]Zwei[/li][/ol]

            [center]Zentrierter Text[/center]

            [hr]

            [table][tr][td]A1[/td][td]B1[/td][/tr][tr][td]A2[/td][td]B2[/td][/tr][/table]

            [hide]Nur für angemeldete Nutzer[/hide]
        """.trimIndent()
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun post() = composeRule.captureScreen("screens", "bbcode", isDark) {
        val tree = POST.toBBTree(BBArgs(resources = ApplicationProvider.getApplicationContext<TestApplication>().resources, userId = "1"))

        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            BBCodeContent(tree, Modifier.padding(16.dp), userId = "1")
        }
    }
}
