package me.proxer.app.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity

/**
 * Shows a web page if no browser is installed.
 *
 * @author Ruben Gees
 */
class WebViewActivity : ComposeActivity() {

    companion object {
        private const val URL_EXTRA = "url"
        private const val MAX_PROGRESS = 100

        fun navigateTo(context: Activity, url: String) = context.startActivity<WebViewActivity>(URL_EXTRA to url)
    }

    private val url: String
        get() = intent.getSafeStringExtra(URL_EXTRA)

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    override fun Content() {
        var title by remember { mutableStateOf(url) }
        var progress by remember { mutableIntStateOf(0) }
        var canGoBack by remember { mutableStateOf(false) }
        var webView by remember { mutableStateOf<WebView?>(null) }

        BackHandler(enabled = canGoBack) { webView?.goBack() }

        ProxerScaffold(title = title, subtitle = url.takeIf { it != title }, onNavigateUp = ::finish, scrollBehavior = null) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                    canGoBack = view.canGoBack()
                                }

                                override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                    canGoBack = view.canGoBack()
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onReceivedTitle(view: WebView, newTitle: String?) {
                                    newTitle?.takeIf { it.isNotBlank() }?.let { title = it }
                                }
                            }

                            loadUrl(this@WebViewActivity.url)
                            webView = this
                        }
                    },
                    onRelease = { it.destroy() },
                    modifier = Modifier.fillMaxSize()
                )

                if (progress < MAX_PROGRESS) {
                    LinearProgressIndicator(progress = { progress / MAX_PROGRESS.toFloat() }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
