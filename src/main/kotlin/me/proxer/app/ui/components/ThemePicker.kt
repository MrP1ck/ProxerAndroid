package me.proxer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.settings.theme.Theme
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.ThemeVariant
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import me.proxer.app.ui.theme.ProxerTheme

/**
 * Lets the user choose the color preset and the light/dark variant. Used in the onboarding and the settings.
 */
@Composable
fun ThemePicker(
    themeContainer: ThemeContainer,
    onThemeContainerChange: (ThemeContainer) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Theme.available.forEach { theme ->
                ThemeSwatch(
                    theme = theme,
                    isSelected = theme == themeContainer.theme,
                    onClick = { onThemeContainerChange(themeContainer.copy(theme = theme)) }
                )
            }
        }

        val variants = listOf(ThemeVariant.LIGHT, ThemeVariant.SYSTEM, ThemeVariant.DARK)

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            variants.forEachIndexed { index, variant ->
                SegmentedButton(
                    selected = variant == themeContainer.variant,
                    onClick = { onThemeContainerChange(themeContainer.copy(variant = variant)) },
                    shape = SegmentedButtonDefaults.itemShape(index, variants.size),
                    icon = {
                        Icon(
                            painter = painterResource(
                                when (variant) {
                                    ThemeVariant.LIGHT -> R.drawable.ic_symbol_light_mode
                                    ThemeVariant.DARK -> R.drawable.ic_symbol_dark_mode
                                    ThemeVariant.SYSTEM -> R.drawable.ic_symbol_brightness_auto
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                ) {
                    Text(stringResource(variant.variantName ?: R.string.theme_variant_system))
                }
            }
        }
    }
}

/**
 * A preview of a color preset: its primary, secondary and tertiary colors in a circle.
 */
@Composable
fun ThemeSwatch(theme: Theme, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = stringResource(theme.themeName)

    Column(
        modifier = modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProxerTheme(ThemeContainer(theme, ThemeVariant.SYSTEM)) {
            val colors = MaterialTheme.colorScheme

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) colors.primary else colors.outlineVariant,
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Column(Modifier.size(44.dp).clip(CircleShape)) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(colors.primary)
                    )

                    Row(Modifier.weight(1f)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .size(22.dp)
                                .background(colors.secondaryContainer)
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .size(22.dp)
                                .background(colors.tertiary)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_check),
                        contentDescription = null,
                        tint = colors.onPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                    )
                }
            }
        }

        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@ThemePreviews
@Composable
private fun ThemePickerPreview() = PreviewSurface {
    ThemePicker(ThemeContainer(Theme.CLASSIC, ThemeVariant.SYSTEM), onThemeContainerChange = {})
}
