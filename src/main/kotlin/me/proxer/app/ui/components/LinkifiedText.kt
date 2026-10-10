package me.proxer.app.ui.components

import android.util.Patterns
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl

/**
 * Turns the web links in [text] into clickable links, which call [onLinkClick]. Replaces the linkify of the Views.
 */
@Composable
fun rememberLinkifiedText(text: String, onLinkClick: (HttpUrl) -> Unit): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary

    return remember(text, linkColor, onLinkClick) { linkify(text, linkColor, onLinkClick) }
}

fun linkify(text: String, linkColor: Color, onLinkClick: (HttpUrl) -> Unit): AnnotatedString = buildAnnotatedString {
    append(text)

    val styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
    val matcher = Patterns.WEB_URL.matcher(text)

    while (matcher.find()) {
        val url = matcher.group().toPrefixedUrlOrNull() ?: continue

        addLink(
            LinkAnnotation.Clickable(tag = url.toString(), styles = styles) { onLinkClick(url) },
            matcher.start(),
            matcher.end()
        )
    }
}
