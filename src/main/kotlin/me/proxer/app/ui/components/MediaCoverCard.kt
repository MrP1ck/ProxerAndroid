package me.proxer.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import okhttp3.HttpUrl

/** The aspect ratio of the cover images on Proxer. */
const val COVER_ASPECT_RATIO = 0.7f

/**
 * A card with a cover image and a title below, for grids of anime and manga.
 */
@Composable
fun MediaCoverCard(
    coverUrl: HttpUrl?,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    overlay: @Composable () -> Unit = {}
) {
    Card(onClick = onClick, modifier = modifier) {
        Column {
            Box {
                ProxerAsyncImage(
                    url = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(COVER_ASPECT_RATIO)
                )

                overlay()
            }

            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun MediaCoverCardPreview() = PreviewSurface {
    MediaCoverCard(
        coverUrl = null,
        title = "Shingeki no Kyojin: The Final Season",
        subtitle = "Animeserie · 16 Episoden",
        onClick = {},
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(0.5f)
    )
}
