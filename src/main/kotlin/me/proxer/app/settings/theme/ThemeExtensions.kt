package me.proxer.app.settings.theme

import android.app.Activity
import androidx.annotation.StyleRes
import com.google.android.material.color.DynamicColors
import me.proxer.app.R

/**
 * Applies the given [ThemeContainer] to this Activity. Needs to be called before the views are inflated.
 *
 * [style] is the style providing the color roles.
 */
fun Activity.applyThemeContainer(container: ThemeContainer, @StyleRes style: Int = container.theme.main) {
    theme.applyStyle(style, true)

    if (container.theme == Theme.DYNAMIC) {
        DynamicColors.applyToActivityIfAvailable(this)
    }

    if (container.isAmoled) {
        theme.applyStyle(R.style.ThemeOverlay_App_Amoled, true)
    }
}
