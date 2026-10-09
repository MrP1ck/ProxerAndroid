package me.proxer.app.ui

import android.app.Activity
import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.intentFor
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import okhttp3.HttpUrl

/**
 * Shows an image in full screen. It can be zoomed with gestures and double taps.
 *
 * @author Ruben Gees
 */
class ImageDetailActivity : ComposeActivity() {

    companion object {
        private const val URL_EXTRA = "url"

        @Suppress("UNUSED_PARAMETER")
        fun navigateTo(context: Activity, url: HttpUrl, imageView: ImageView? = null) {
            context.startActivity(context.intentFor<ImageDetailActivity>(URL_EXTRA to url.toString()))
        }
    }

    override val hasDarkSystemBars = true

    private val url: String
        get() = intent.getSafeStringExtra(URL_EXTRA)

    @Composable
    override fun Content() {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            ZoomableAsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onClick = { finish() }
            )

            IconButton(
                onClick = { finish() },
                modifier = Modifier
                    .safeDrawingPadding()
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    painterResource(R.drawable.ic_symbol_close),
                    contentDescription = stringResource(R.string.action_navigate_up),
                    tint = Color.White
                )
            }
        }
    }
}
