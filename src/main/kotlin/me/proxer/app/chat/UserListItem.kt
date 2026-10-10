package me.proxer.app.chat

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proxer.app.comment.userImageUrl
import me.proxer.app.ui.components.UserAvatar
import me.proxer.app.ui.components.rememberLinkifiedText
import me.proxer.app.ui.shell.LocalAppNavigator

/**
 * A user in the participant list of a conference or chat room, with the (linkified) [status] and an optional [badge]
 * (e.g. for the leader or the moderators).
 */
@Composable
fun UserListItem(
    username: String,
    image: String,
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes badge: Int? = null,
    badgeDescription: String? = null
) {
    val navigator = LocalAppNavigator.current

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    username,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (badge != null) {
                    Spacer(Modifier.width(4.dp))

                    Icon(
                        painter = painterResource(badge),
                        contentDescription = badgeDescription,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        supportingContent = status.trim().takeIf { it.isNotEmpty() }?.let {
            {
                Text(
                    rememberLinkifiedText(it) { url -> navigator.showPage(url) },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        leadingContent = { UserAvatar(userImageUrl(image)) },
        modifier = modifier.clickable(onClick = onClick)
    )
}
