package me.proxer.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A row with a title and its content, e.g. a fact of an entry or a character.
 */
@Composable
fun InfoRow(title: String, content: String, isSelectable: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp)
        )

        val contentModifier = Modifier
            .weight(1f)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }

        val contentText: @Composable () -> Unit = {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = contentModifier
            )
        }

        if (isSelectable) SelectionContainer(contentModifier) { contentText() } else contentText()
    }
}
