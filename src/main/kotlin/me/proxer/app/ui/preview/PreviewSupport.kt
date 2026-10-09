package me.proxer.app.ui.preview

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import me.proxer.app.settings.theme.Theme
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.ui.theme.ProxerTheme

/**
 * Previews a composable in the light and the dark theme.
 */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class ThemePreviews

/**
 * Previews a composable on a phone, a foldable and a tablet.
 */
@Preview(name = "Phone", device = "spec:width=411dp,height=891dp", showBackground = true)
@Preview(name = "Foldable", device = "spec:width=673dp,height=841dp", showBackground = true)
@Preview(name = "Tablet", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true)
annotation class ScreenSizePreviews

/**
 * Wraps preview content in the app theme and a surface, so text and background have the right colors.
 */
@Composable
fun PreviewSurface(
    theme: Theme = Theme.CLASSIC,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit
) {
    ProxerTheme(ThemeContainer(theme, ThemeVariant.SYSTEM, isAmoled)) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
