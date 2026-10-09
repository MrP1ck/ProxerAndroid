package me.proxer.app.settings.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.view.ContextThemeWrapper
import com.google.android.material.color.DynamicColors
import me.proxer.app.R
import me.proxer.app.util.extension.resolveColor

/**
 * @author Ruben Gees
 */
@Suppress("unused")
enum class Theme(
    val preferenceId: String,
    @StringRes val themeName: Int,
    @StyleRes val main: Int,
    @StyleRes val noBackground: Int
) {
    /**
     * Uses the colors of the wallpaper (Material You). Only available on Android 12 and newer, see [isAvailable].
     * The colors of [CLASSIC] are used as the base, the system colors are applied on top.
     */
    DYNAMIC(
        "3",
        R.string.theme_dynamic,
        R.style.Theme_App,
        R.style.Theme_App_NoBackground
    ),
    CLASSIC(
        "0",
        R.string.theme_classic,
        R.style.Theme_App,
        R.style.Theme_App_NoBackground
    ),
    BLUE_GREEN(
        "1",
        R.string.theme_bg,
        R.style.Theme_App_BG,
        R.style.Theme_App_BG_NoBackground
    ),
    GLOOMY(
        "2",
        R.string.theme_gloomy,
        R.style.Theme_App_Gloomy,
        R.style.Theme_App_Gloomy_NoBackground
    );

    companion object {
        val available get() = values().filter { it.isAvailable }
    }

    val isAvailable get() = this != DYNAMIC || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @ColorInt
    fun primaryColor(context: Context): Int {
        return themedContext(context).resolveColor(R.attr.colorPrimary)
    }

    @ColorInt
    fun secondaryColor(context: Context): Int {
        return themedContext(context).resolveColor(R.attr.colorSecondaryContainer)
    }

    @ColorInt
    fun colorOnSecondary(context: Context): Int {
        return themedContext(context).resolveColor(R.attr.colorOnSecondaryContainer)
    }

    private fun themedContext(context: Context): Context {
        val themed = ContextThemeWrapper(context, main)

        return when (this) {
            DYNAMIC -> DynamicColors.wrapContextIfAvailable(themed)
            else -> themed
        }
    }
}
