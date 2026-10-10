package me.proxer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import me.proxer.app.R
import me.proxer.app.TestApplication
import me.proxer.app.settings.theme.Theme
import me.proxer.app.ui.components.ChipRow
import me.proxer.app.ui.components.EmptyState
import me.proxer.app.ui.components.ErrorState
import me.proxer.app.ui.components.MediaCoverCard
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the design system components for every theme preset, in light and dark mode.
 * Record with ./gradlew recordRoborazziDebug, verify with ./gradlew verifyRoborazziDebug.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class DesignSystemScreenshotTest(private val theme: Theme, private val isDark: Boolean, private val isAmoled: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_dark={1}_amoled={2}")
        fun parameters() = Theme.values().filter { it != Theme.DYNAMIC }.flatMap { theme ->
            listOf(arrayOf<Any>(theme, false, false), arrayOf<Any>(theme, true, false), arrayOf<Any>(theme, true, true))
        }
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun components() {
        if (isDark) RuntimeEnvironmentHelper.setNightMode()

        composeRule.setContent {
            PreviewSurface(theme = theme, isAmoled = isAmoled) {
                ComponentsSample()
            }
        }

        val name = "${theme.name.lowercase()}_${if (isDark) "dark" else "light"}${if (isAmoled) "_amoled" else ""}"

        composeRule.onRoot().captureRoboImage("src/test/screenshots/design_system/$name.png")
    }
}

@Composable
private fun ComponentsSample() {
    Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("Buttons")

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("Filled") }
            FilledTonalButton(onClick = {}) { Text("Tonal") }
            OutlinedButton(onClick = {}) { Text("Outlined") }
        }

        SectionHeader("Chips")

        ChipRow(Modifier.padding(horizontal = 16.dp)) {
            listOf("Action", "Abenteuer", "Comedy", "Drama", "Fantasy").forEach {
                AssistChip(onClick = {}, label = { Text(it) })
            }
        }

        SectionHeader("Cards")

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MediaCoverCard(
                coverUrl = null,
                title = "Shingeki no Kyojin",
                subtitle = "Animeserie",
                onClick = {},
                modifier = Modifier.width(140.dp)
            )

            Card(Modifier.fillMaxWidth()) {
                Text(
                    text = "Ein Kommentar in einer Karte, mit etwas mehr Text.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Row(Modifier.height(320.dp)) {
            ErrorState(
                ErrorAction(
                    R.string.error_no_network,
                    R.string.error_action_network_settings,
                    ButtonAction.NETWORK_SETTINGS
                ),
                onAction = {},
                modifier = Modifier.weight(1f)
            )

            EmptyState(R.string.error_no_data_bookmark, Modifier.weight(1f))
        }
    }
}
