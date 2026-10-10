package me.proxer.app.settings

import android.content.SharedPreferences
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.DialogButton

private const val DISABLED_ALPHA = 0.38f

/**
 * Observes the value of [key] in these preferences. [read] is called initially and after every change of the key.
 */
@Composable
fun <T> SharedPreferences.observeAsState(key: String, read: SharedPreferences.() -> T): State<T> {
    val state = remember(this, key) { mutableStateOf(read()) }

    DisposableEffect(this, key) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, changedKey ->
            if (changedKey == key || changedKey == null) {
                state.value = preferences.read()
            }
        }

        state.value = read()
        registerOnSharedPreferenceChangeListener(listener)

        onDispose { unregisterOnSharedPreferenceChangeListener(listener) }
    }

    return state
}

/**
 * A plain preference which does something when clicked, e.g. opening another screen.
 */
@Composable
fun Preference(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = icon?.let { { Icon(painterResource(it), contentDescription = null) } },
        modifier = modifier
            .let { if (onClick != null) it.clickable(enabled = enabled, onClick = onClick) else it }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    )
}

/**
 * A preference with a switch. The summary depends on the state, like the summaryOn/summaryOff of the old preferences.
 */
@Composable
fun SwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = modifier
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
    )
}

/**
 * A preference to choose one of [entries]. The current entry is shown as summary, the choices in a dialog.
 */
@Composable
fun <T> ListPreference(
    title: String,
    entries: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: (String) -> String = { it }
) {
    var isDialogVisible by rememberSaveable { mutableStateOf(false) }
    val selectedTitle = entries.find { it.first == selected }?.second ?: ""

    Preference(
        title = title,
        summary = summary(selectedTitle),
        enabled = enabled,
        onClick = { isDialogVisible = true },
        modifier = modifier
    )

    if (isDialogVisible) {
        AlertDialog(
            onDismissRequest = { isDialogVisible = false },
            title = { Text(title) },
            text = {
                Column(Modifier.selectableGroup()) {
                    entries.forEach { (value, entryTitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = value == selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        isDialogVisible = false

                                        if (value != selected) onSelect(value)
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = value == selected, onClick = null)

                            Text(
                                text = entryTitle,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { DialogButton(R.string.cancel, onClick = { isDialogVisible = false }) }
        )
    }
}

/**
 * The header of a group of preferences.
 */
@Composable
fun PreferenceCategory(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

/**
 * The choices of a [ListPreference] from the string arrays of the [values] and their [titles].
 */
@Composable
internal fun choices(@ArrayRes titles: Int, @ArrayRes values: Int): List<Pair<String, String>> =
    stringArrayResource(values).zip(stringArrayResource(titles))

@Composable
internal fun summaryOf(
    checked: Boolean,
    @StringRes on: Int,
    @StringRes off: Int
) = stringResource(if (checked) on else off).trim()
