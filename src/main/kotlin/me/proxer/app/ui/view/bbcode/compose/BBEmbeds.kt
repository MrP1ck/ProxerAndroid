package me.proxer.app.ui.view.bbcode.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proxer.app.ui.view.ProxerWebView
import me.proxer.app.util.extension.buildSingle
import me.proxer.library.ProxerApi
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.context.GlobalContext
import timber.log.Timber
import java.io.File

private const val MAX_PDF_PAGES = 10
private const val PDF_RENDER_WIDTH = 1080

/**
 * The pages of a PDF, rendered as images.
 */
@Composable
internal fun BBPdf(url: HttpUrl?, width: Int?) {
    val context = LocalContext.current
    val density = LocalDensity.current

    if (url == null) return

    val pages by produceState<List<ImageBitmap>?>(null, url) {
        value = withContext(Dispatchers.IO) {
            runCatching { renderPdf(context, url) }.onFailure { Timber.e(it) }.getOrDefault(emptyList())
        }
    }

    Column(
        modifier = Modifier.let { if (width != null) it.widthIn(max = with(density) { width.toDp() }) else it.fillMaxWidth() },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (val safePages = pages) {
            null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> safePages.forEach { page ->
                Image(page, contentDescription = null, contentScale = ContentScale.FillWidth, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun renderPdf(context: Context, url: HttpUrl): List<ImageBitmap> {
    val client = GlobalContext.getOrNull()?.getOrNull<OkHttpClient>() ?: OkHttpClient()
    val file = File(context.cacheDir, "bbcode_pdf/${url.toString().hashCode()}.pdf")

    if (!file.exists()) {
        file.parentFile?.mkdirs()

        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            val body = response.body ?: error("Empty response")

            file.outputStream().use { body.byteStream().copyTo(it) }
        }
    }

    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            (0 until minOf(renderer.pageCount, MAX_PDF_PAGES)).map { index ->
                renderer.openPage(index).use { page ->
                    val height = (PDF_RENDER_WIDTH.toFloat() / page.width * page.height).toInt()
                    val bitmap = Bitmap.createBitmap(PDF_RENDER_WIDTH, height, Bitmap.Config.ARGB_8888)

                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    bitmap.asImageBitmap()
                }
            }
        }
    }
}

/**
 * A page of the Proxer wiki, loaded and shown as HTML.
 */
@Composable
internal fun BBWiki(link: String) {
    val environment = LocalBBCodeEnvironment.current

    val html by produceState<String?>(null, link) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                GlobalContext.getOrNull()?.getOrNull<ProxerApi>()?.wiki?.content(link)?.buildSingle()?.blockingGet()?.content
            }.onFailure { Timber.e(it) }.getOrNull()
        }
    }

    html?.let { content ->
        AndroidView(
            factory = { context ->
                ProxerWebView(context).apply { showPageSubject.subscribe { environment.onUrl(it) } }
            },
            update = { it.loadHtml(content) },
            onRelease = { it.destroy() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
