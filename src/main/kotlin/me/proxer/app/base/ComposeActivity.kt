package me.proxer.app.base

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.applyThemeContainer
import me.proxer.app.ui.shell.AppNavigator
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.theme.ProxerAppTheme

/**
 * An Activity showing a Compose screen. Used for the screens which are opened through intents from outside of the main
 * navigation, e.g. from deep links, notifications and the screens which are still View based.
 */
abstract class ComposeActivity : BaseActivity() {

    /**
     * Whether the system bars always use light icons, e.g. for screens with a black background.
     */
    protected open val hasDarkSystemBars = false

    @Composable
    protected abstract fun Content()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (hasDarkSystemBars) {
            true -> enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
            )
            false -> enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            ProxerAppTheme {
                val navigator = remember { AppNavigator(this, null) }

                CompositionLocalProvider(LocalAppNavigator provides navigator) {
                    Content()
                }
            }
        }
    }

    override fun onThemeChanged(themeContainer: ThemeContainer) {
        applyThemeContainer(themeContainer)
    }
}
