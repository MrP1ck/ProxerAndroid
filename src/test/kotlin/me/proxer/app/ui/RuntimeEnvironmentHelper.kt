package me.proxer.app.ui

import org.robolectric.RuntimeEnvironment

object RuntimeEnvironmentHelper {

    /** Switches the Robolectric configuration to night mode, which isSystemInDarkTheme() reads. */
    fun setNightMode() {
        RuntimeEnvironment.setQualifiers("+night")
    }
}
