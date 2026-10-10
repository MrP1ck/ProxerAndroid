package me.proxer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import okhttp3.HttpUrl

/**
 * The round avatar of a user. Without [url], a placeholder icon is shown.
 */
@Composable
fun UserAvatar(url: HttpUrl?, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (url == null) {
            Icon(
                painterResource(R.drawable.ic_symbol_person),
                null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            ProxerAsyncImage(url, null, Modifier.size(size), showErrorIcon = false)
        }
    }
}
