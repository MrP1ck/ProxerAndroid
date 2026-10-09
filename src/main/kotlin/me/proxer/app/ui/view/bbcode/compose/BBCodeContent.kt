package me.proxer.app.ui.view.bbcode.compose

import android.content.ActivityNotFoundException
import android.text.style.ClickableSpan
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBCodeEmoticons
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.extension.toast
import okhttp3.HttpUrl

/**
 * What BBCode needs from the app: the state of the user and how to open links, images and profiles.
 */
class BBCodeEnvironment(
    val isLoggedIn: Boolean,
    val isAgeRestrictedMediaAllowed: Boolean,
    val onUrl: (HttpUrl) -> Unit,
    val onMention: (String) -> Unit,
    val onImage: (HttpUrl) -> Unit,
    val expandSpoilers: Boolean = false
)

val LocalBBCodeEnvironment = staticCompositionLocalOf {
    BBCodeEnvironment(
        isLoggedIn = false,
        isAgeRestrictedMediaAllowed = false,
        onUrl = {},
        onMention = {},
        onImage = {}
    )
}

/** Whether text is rendered with a monospace font, e.g. inside a code tag. */
private val LocalIsMonospace = staticCompositionLocalOf { false }

/**
 * Renders the BBCode [tree]. [userId] is the author, needed for attachments. [enableEmoticons] replaces the Proxer
 * emoticon codes with their animations.
 */
@Composable
fun BBCodeContent(
    tree: BBTree,
    modifier: Modifier = Modifier,
    userId: String? = null,
    enableEmoticons: Boolean = false
) {
    val resources = LocalContext.current.resources
    val blocks = remember(tree, userId) { tree.toBlocks(BBArgs(resources = resources, userId = userId)) }

    Blocks(merge(blocks), enableEmoticons, modifier)
}

@Composable
private fun Blocks(blocks: List<BBBlock>, enableEmoticons: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { Block(it, enableEmoticons) }
    }
}

@Composable
private fun Block(block: BBBlock, enableEmoticons: Boolean) {
    val environment = LocalBBCodeEnvironment.current

    when (block) {
        is BBBlock.Text -> BBText(block.text, enableEmoticons)
        is BBBlock.Image -> BBImage(block.url, block.width)
        is BBBlock.Pdf -> BBPdf(block.url, block.width)
        is BBBlock.Wiki -> BBWiki(block.link)
        is BBBlock.Divider -> HorizontalDivider(thickness = 2.dp)
        is BBBlock.Spoiler -> BBSpoiler(block, enableEmoticons)
        is BBBlock.Quote -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (block.quotedUser != null) {
                Text(
                    text = stringResource(R.string.view_bbcode_quote, block.quotedUser.trim()),
                    style = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.clickable { environment.onMention(block.quotedUser.trim()) }
                )
            }

            if (block.children.isNotEmpty()) {
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Blocks(block.children, enableEmoticons, Modifier.padding(8.dp))
                    }
                }
            }
        }
        is BBBlock.Code -> Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalIsMonospace provides true) {
                Blocks(block.children, enableEmoticons, Modifier.padding(8.dp))
            }
        }
        is BBBlock.Restricted -> BBRestricted(block, enableEmoticons)
        is BBBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEachIndexed { index, item ->
                Row {
                    Text(
                        text = if (block.isOrdered) "${index + 1}." else "•",
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Blocks(item, enableEmoticons, Modifier.weight(1f))
                }
            }
        }
        is BBBlock.Table -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val columnCount = block.rows.maxOfOrNull { it.size } ?: 1

            block.rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.forEach { cell -> Blocks(cell, enableEmoticons, Modifier.weight(1f)) }

                    repeat(columnCount - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
        is BBBlock.Aligned -> Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = when (block.alignment) {
                BBBlock.Alignment.START -> Alignment.CenterStart
                BBBlock.Alignment.CENTER -> Alignment.Center
                BBBlock.Alignment.END -> Alignment.CenterEnd
            }
        ) {
            Blocks(block.children, enableEmoticons)
        }
        is BBBlock.Linked -> Box(Modifier.clickable { environment.onUrl(block.url) }) {
            Blocks(block.children, enableEmoticons)
        }
    }
}

@Composable
private fun BBText(text: CharSequence, enableEmoticons: Boolean) {
    val environment = LocalBBCodeEnvironment.current
    val view = LocalView.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val linkColor = MaterialTheme.colorScheme.primary
    val isMonospace = LocalIsMonospace.current

    val linkHandler = remember(environment, view) {
        BBLinkHandler(
            onUrl = { url ->
                when {
                    url.startsWith("@") -> environment.onMention(url.trim().drop(1))
                    else -> url.toPrefixedUrlOrNull()?.let(environment.onUrl)
                }
            },
            onClickableSpan = { span: ClickableSpan ->
                try {
                    span.onClick(view)
                } catch (error: ActivityNotFoundException) {
                    context.toast(R.string.view_bbcode_map_no_activity_error)
                }
            }
        )
    }

    val annotated = remember(text, linkColor, density, linkHandler) {
        text.toAnnotatedString(linkColor, density, linkHandler)
    }

    val emoticons = remember(annotated, enableEmoticons) {
        if (enableEmoticons) BBCodeEmoticons.find(annotated.text) else emptyList()
    }

    val style = LocalTextStyle.current.let {
        if (isMonospace) it.copy(fontFamily = FontFamily.Monospace) else it
    }

    if (emoticons.isEmpty()) {
        Text(annotated, style = style, textAlign = text.textAlign() ?: TextAlign.Start, modifier = Modifier.fillMaxWidth())
    } else {
        val withEmoticons = remember(annotated, emoticons) { annotated.replaceEmoticons(emoticons) }
        val inlineContent = emoticons.associate { (_, drawable) ->
            "emoticon_$drawable" to InlineTextContent(
                Placeholder(2.em, 2.em, PlaceholderVerticalAlign.TextCenter)
            ) {
                AsyncImage(model = drawable, contentDescription = null)
            }
        }

        Text(
            withEmoticons,
            style = style,
            inlineContent = inlineContent,
            textAlign = text.textAlign() ?: TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun AnnotatedString.replaceEmoticons(emoticons: List<Pair<IntRange, Int>>) = buildAnnotatedString {
    var lastEnd = 0

    emoticons.sortedBy { it.first.first }.forEach { (range, drawable) ->
        append(this@replaceEmoticons.subSequence(lastEnd, range.first))
        appendInlineContent("emoticon_$drawable", this@replaceEmoticons.text.substring(range))

        lastEnd = range.last + 1
    }

    append(this@replaceEmoticons.subSequence(lastEnd, this@replaceEmoticons.length))
}

@Composable
private fun BBImage(url: HttpUrl?, width: Int?) {
    val environment = LocalBBCodeEnvironment.current
    val density = LocalDensity.current

    if (url == null) return

    AsyncImage(
        model = url.toString(),
        contentDescription = null,
        modifier = Modifier
            .let { modifier -> if (width != null) modifier.widthIn(max = with(density) { width.toDp() }) else modifier }
            .heightIn(max = 600.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable { environment.onImage(url) }
    )
}

@Composable
private fun BBSpoiler(block: BBBlock.Spoiler, enableEmoticons: Boolean) {
    val expandByDefault = LocalBBCodeEnvironment.current.expandSpoilers
    var isExpanded by rememberSaveable(block) { mutableStateOf(expandByDefault) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.title ?: stringResource(
                        if (isExpanded) R.string.view_bbcode_hide_spoiler else R.string.view_bbcode_show_spoiler
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painterResource(R.drawable.ic_symbol_expand_more),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                )
            }

            AnimatedVisibility(isExpanded) {
                Blocks(block.children, enableEmoticons, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            }
        }
    }
}

@Composable
private fun BBRestricted(block: BBBlock.Restricted, enableEmoticons: Boolean) {
    val environment = LocalBBCodeEnvironment.current
    val isVisible = if (block.requiresLogin) environment.isLoggedIn else environment.isAgeRestrictedMediaAllowed

    if (!isVisible) {
        Text(
            text = stringResource(
                if (block.requiresLogin) R.string.view_bbcode_hide_login else R.string.view_bbcode_hide_age_restricted
            ),
            textAlign = TextAlign.Center,
            color = LocalContentColor.current.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        if (block.requiresLogin) R.string.view_bbcode_login else R.string.view_bbcode_age_restricted
                    ),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Blocks(block.children, enableEmoticons)
            }
        }
    }
}
