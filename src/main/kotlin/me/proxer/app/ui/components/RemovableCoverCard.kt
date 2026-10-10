package me.proxer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import okhttp3.HttpUrl

/**
 * A [MediaCoverCard] with an optional button to remove the entry, e.g. from the own favorites, and a rating badge.
 */
@Composable
fun RemovableCoverCard(
    coverUrl: HttpUrl,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onRemove: (() -> Unit)?,
    modifier: Modifier = Modifier,
    rating: Float = 0f
) {
    MediaCoverCard(
        coverUrl = coverUrl,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        overlay = {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(4.dp)
            ) {
                if (rating > 0) RatingBadge(rating, Modifier.padding(4.dp))

                if (onRemove != null) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_symbol_close),
                            contentDescription = stringResource(R.string.action_remove),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    )
}
