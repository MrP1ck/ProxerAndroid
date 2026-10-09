package me.proxer.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import me.proxer.app.settings.theme.Theme
import me.proxer.app.ui.preview.PreviewSurface

/**
 * Renders [content] in the app theme and records it as src/test/screenshots/<group>/<name>.png.
 */
fun ComposeContentTestRule.captureScreen(
    group: String,
    name: String,
    isDark: Boolean = false,
    theme: Theme = Theme.CLASSIC,
    content: @Composable () -> Unit
) {
    if (isDark) RuntimeEnvironmentHelper.setNightMode()

    setContent { PreviewSurface(theme = theme) { content() } }

    onRoot().captureRoboImage("src/test/screenshots/$group/$name${if (isDark) "_dark" else ""}.png")
}
