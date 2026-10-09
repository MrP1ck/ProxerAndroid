package me.proxer.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The surface of a Material 3 dialog with an optional [icon] and [title], the scrollable [content] and the buttons.
 * Used by dialogs which are not composables themselves (see [me.proxer.app.base.ComposeDialog]).
 *
 * The [neutralButton] is placed at the start, the [dismissButton] and [confirmButton] at the end.
 */
@Composable
fun ProxerDialogContent(
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    icon: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    neutralButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.widthIn(min = 280.dp, max = 560.dp),
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation
    ) {
        Column(Modifier.padding(top = 24.dp, bottom = 18.dp)) {
            if (icon != null) {
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.iconContentColor) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 16.dp)
                    ) {
                        icon()
                    }
                }
            }

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp)
                        .align(if (icon != null) Alignment.CenterHorizontally else Alignment.Start)
                )
            }

            Box(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium, content)
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (neutralButton != null) {
                    neutralButton()

                    Box(Modifier.weight(1f))
                }

                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

/**
 * A text button of a dialog.
 */
@Composable
fun DialogButton(@StringRes text: Int, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(stringResource(text))
    }
}

/**
 * A checkbox with a clickable label, e.g. for "don't ask again".
 */
@Composable
fun LabeledCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)

        Spacer(Modifier.width(12.dp))

        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
