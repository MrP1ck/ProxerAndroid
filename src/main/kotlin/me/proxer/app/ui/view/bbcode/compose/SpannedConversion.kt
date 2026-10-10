package me.proxer.app.ui.view.bbcode.compose

import android.graphics.Typeface
import android.text.Layout
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * How links in converted text are handled. [onUrl] receives the target of web links and mentions (starting with @),
 * [onClickableSpan] other clickable spans, like links to maps.
 */
class BBLinkHandler(
    val onUrl: (String) -> Unit,
    val onClickableSpan: (ClickableSpan) -> Unit
)

/**
 * Converts text styled with Android spans (as produced by the BBCode prototypes) into an [AnnotatedString].
 */
fun CharSequence.toAnnotatedString(linkColor: Color, density: Density, linkHandler: BBLinkHandler): AnnotatedString {
    val spanned = this as? Spanned ?: return AnnotatedString(toString())
    val linkStyles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))

    return buildAnnotatedString {
        append(spanned.toString())

        spanned.getSpans(0, spanned.length, Any::class.java).forEach { span ->
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)

            if (start < 0 || end <= start) return@forEach

            when (span) {
                is StyleSpan -> addStyle(
                    when (span.style) {
                        Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
                        Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
                        Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
                        else -> SpanStyle()
                    },
                    start,
                    end
                )
                is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                is StrikethroughSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
                is RelativeSizeSpan -> addStyle(SpanStyle(fontSize = span.sizeChange.em), start, end)
                is AbsoluteSizeSpan -> addStyle(
                    SpanStyle(
                        fontSize = when (span.dip) {
                            true -> span.size.sp
                            false -> with(density) { span.size.toSp() }
                        }
                    ),
                    start,
                    end
                )
                is SubscriptSpan -> addStyle(
                    SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.75.em),
                    start,
                    end
                )
                is SuperscriptSpan -> addStyle(
                    SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em),
                    start,
                    end
                )
                is URLSpan -> addLink(
                    LinkAnnotation.Clickable(span.url, linkStyles) { linkHandler.onUrl(span.url) },
                    start,
                    end
                )
                is ClickableSpan -> addLink(
                    LinkAnnotation.Clickable(span.toString(), linkStyles) { linkHandler.onClickableSpan(span) },
                    start,
                    end
                )
            }
        }
    }
}

/**
 * The alignment of the text, if it is set for all of it with an [AlignmentSpan].
 */
fun CharSequence.textAlign(): TextAlign? {
    val spanned = this as? Spanned ?: return null

    return spanned.getSpans(0, spanned.length, AlignmentSpan::class.java)
        .lastOrNull { spanned.getSpanStart(it) == 0 }
        ?.let {
            when (it.alignment) {
                Layout.Alignment.ALIGN_CENTER -> TextAlign.Center
                Layout.Alignment.ALIGN_OPPOSITE -> TextAlign.End
                else -> TextAlign.Start
            }
        }
}
