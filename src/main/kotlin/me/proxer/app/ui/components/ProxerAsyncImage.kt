package me.proxer.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import me.proxer.app.R
import okhttp3.HttpUrl

/**
 * Loads an image with Coil. While loading or if loading failed, a tonal placeholder is shown instead.
 */
@Composable
fun ProxerAsyncImage(
    url: HttpUrl?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showErrorIcon: Boolean = true
) {
    var state by remember(url) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    Box(modifier) {
        if (state !is AsyncImagePainter.State.Success) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (showErrorIcon && (url == null || state is AsyncImagePainter.State.Error)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_image),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (url != null) {
            AsyncImage(
                model = url.toString(),
                contentDescription = contentDescription,
                modifier = Modifier.matchParentSize(),
                contentScale = contentScale,
                onState = { state = it }
            )
        }
    }
}
