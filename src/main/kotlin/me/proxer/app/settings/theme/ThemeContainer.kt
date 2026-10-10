package me.proxer.app.settings.theme

/**
 * @author Ruben Gees
 */
data class ThemeContainer(val theme: Theme, val variant: ThemeVariant, val isAmoled: Boolean = false) {

    companion object {
        private const val DELIMITER = "_"
        private const val AMOLED = "1"

        fun fromPreferenceString(value: String): ThemeContainer {
            val split = value.split(DELIMITER)

            val theme = Theme.values()
                .find { it.preferenceId == split.getOrNull(0) }
                ?.takeIf { it.isAvailable }
                ?: Theme.CLASSIC

            val variant = ThemeVariant.values().find { it.preferenceId == split.getOrNull(1) } ?: ThemeVariant.SYSTEM
            val isAmoled = split.getOrNull(2) == AMOLED

            return ThemeContainer(theme, variant, isAmoled)
        }
    }

    fun toPreferenceString() = when (isAmoled) {
        true -> "${theme.preferenceId}$DELIMITER${variant.preferenceId}$DELIMITER$AMOLED"
        false -> "${theme.preferenceId}$DELIMITER${variant.preferenceId}"
    }
}
