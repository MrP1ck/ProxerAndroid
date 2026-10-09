package me.proxer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proxer.app.R

/**
 * The texts of [MediaControls], which differ between episodes and chapters.
 */
data class MediaControlTexts(
    val previous: String,
    val next: String,
    val bookmarkThis: String,
    val bookmarkNext: String
)

/**
 * An info row of [MediaControls], e.g. the uploader of a chapter. [onClick] makes the value clickable.
 */
data class MediaControlInfo(val label: String, val value: String, val onClick: (() -> Unit)? = null)

/**
 * The controls to switch to the previous or next episode (or chapter) and to bookmark it. When the [current] one is the
 * last one, the entry can be marked as finished instead of bookmarking the next one. Replaces the MediaControlView.
 */
@Composable
fun MediaControls(
    current: Int,
    amount: Int,
    texts: MediaControlTexts,
    onSwitch: (Int) -> Unit,
    onBookmark: (Int) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    info: List<MediaControlInfo> = emptyList()
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        info.forEach { row ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 16.dp)
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = row.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (row.onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = row.onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (current > 1) {
                OutlinedButton(
                    onClick = { onSwitch(current - 1) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_arrow_back),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(texts.previous, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (current < amount) {
                FilledTonalButton(
                    onClick = { onSwitch(current + 1) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(texts.next, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Icon(
                        painterResource(R.drawable.ic_symbol_arrow_forward),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { onBookmark(current) }) {
                Icon(
                    painterResource(R.drawable.ic_symbol_bookmark_add),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(texts.bookmarkThis)
            }

            if (current < amount) {
                TextButton(onClick = { onBookmark(current + 1) }) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_bookmark_add),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(texts.bookmarkNext)
                }
            } else {
                TextButton(onClick = onFinish) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_done_all),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.view_media_control_finish))
                }
            }
        }
    }
}
