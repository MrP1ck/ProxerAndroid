package me.proxer.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proxer.app.settings.theme.Theme
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.util.data.PreferenceHelper
import org.koin.compose.koinInject

private val ProxerTypography = Typography()

/**
 * The app theme, following the theme the user selected in the settings. Changes are applied immediately.
 */
@Composable
fun ProxerAppTheme(content: @Composable () -> Unit) {
    val preferenceHelper = koinInject<PreferenceHelper>()
    val themeContainer by preferenceHelper.themeFlow.collectAsStateWithLifecycle()

    ProxerTheme(themeContainer, content = content)
}

/**
 * The app theme for the given [themeContainer]. Use [ProxerAppTheme] in the app, this is mainly meant for previews
 * and tests.
 */
@Composable
fun ProxerTheme(
    themeContainer: ThemeContainer = ThemeContainer(Theme.CLASSIC, ThemeVariant.SYSTEM),
    isDark: Boolean = themeContainer.variant.isDark(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = remember(themeContainer, isDark, context) {
        val baseColorScheme = when (themeContainer.theme) {
            Theme.DYNAMIC -> when {
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> classicColorScheme(isDark)
                isDark -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
            Theme.CLASSIC -> classicColorScheme(isDark)
            Theme.BLUE_GREEN -> if (isDark) BlueGreenDarkColorScheme else BlueGreenLightColorScheme
            Theme.GLOOMY -> if (isDark) GloomyDarkColorScheme else GloomyLightColorScheme
        }

        if (isDark && themeContainer.isAmoled) baseColorScheme.toAmoled() else baseColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ProxerTypography,
        content = content
    )
}

@Composable
private fun ThemeVariant.isDark() = when (this) {
    ThemeVariant.LIGHT -> false
    ThemeVariant.DARK -> true
    ThemeVariant.SYSTEM -> isSystemInDarkTheme()
}

private fun classicColorScheme(isDark: Boolean) = if (isDark) ClassicDarkColorScheme else ClassicLightColorScheme

/**
 * Pure black background and surfaces for AMOLED displays. Matches ThemeOverlay.App.Amoled of the View system.
 */
private fun ColorScheme.toAmoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black
)
