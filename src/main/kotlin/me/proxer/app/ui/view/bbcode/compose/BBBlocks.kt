package me.proxer.app.ui.view.bbcode.compose

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.AgeRestrictionPrototype
import me.proxer.app.ui.view.bbcode.prototype.AttachmentPrototype
import me.proxer.app.ui.view.bbcode.prototype.CenterPrototype
import me.proxer.app.ui.view.bbcode.prototype.CodePrototype
import me.proxer.app.ui.view.bbcode.prototype.DividerPrototype
import me.proxer.app.ui.view.bbcode.prototype.HidePrototype
import me.proxer.app.ui.view.bbcode.prototype.ImagePrototype
import me.proxer.app.ui.view.bbcode.prototype.LeftPrototype
import me.proxer.app.ui.view.bbcode.prototype.ListItemPrototype
import me.proxer.app.ui.view.bbcode.prototype.OrderedListPrototype
import me.proxer.app.ui.view.bbcode.prototype.PdfPrototype
import me.proxer.app.ui.view.bbcode.prototype.QuotePrototype
import me.proxer.app.ui.view.bbcode.prototype.RightPrototype
import me.proxer.app.ui.view.bbcode.prototype.SpoilerPrototype
import me.proxer.app.ui.view.bbcode.prototype.TableCellPrototype
import me.proxer.app.ui.view.bbcode.prototype.TablePrototype
import me.proxer.app.ui.view.bbcode.prototype.TableRowPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextMutatorPrototype
import me.proxer.app.ui.view.bbcode.prototype.TextPrototype
import me.proxer.app.ui.view.bbcode.prototype.UnorderedListPrototype
import me.proxer.app.ui.view.bbcode.prototype.UrlPrototype
import me.proxer.app.ui.view.bbcode.prototype.WikiPrototype
import me.proxer.app.util.extension.proxyIfRequired
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl

/**
 * The building blocks of rendered BBCode. The text styles are applied as spans, the structure as blocks.
 */
sealed interface BBBlock {

    data class Text(val text: CharSequence) : BBBlock

    data class Image(val url: HttpUrl?, val width: Int?) : BBBlock

    data class Pdf(val url: HttpUrl?, val width: Int?) : BBBlock

    data class Spoiler(val title: String?, val children: List<BBBlock>) : BBBlock

    data class Quote(val quotedUser: String?, val children: List<BBBlock>) : BBBlock

    data class Code(val children: List<BBBlock>) : BBBlock

    /** Content only visible to logged in users ([requiresLogin]) or users who confirmed their age. */
    data class Restricted(val requiresLogin: Boolean, val children: List<BBBlock>) : BBBlock

    data object Divider : BBBlock

    data class ListBlock(val isOrdered: Boolean, val items: List<List<BBBlock>>) : BBBlock

    data class Table(val rows: List<List<List<BBBlock>>>) : BBBlock

    data class Wiki(val link: String) : BBBlock

    /** Blocks other than text aligned horizontally, e.g. a centered image. */
    data class Aligned(val alignment: Alignment, val children: List<BBBlock>) : BBBlock

    /** Blocks other than text which open a link when clicked, e.g. an image in an url tag. */
    data class Linked(val url: HttpUrl, val children: List<BBBlock>) : BBBlock

    enum class Alignment { START, CENTER, END }
}

/**
 * Converts the (optimized) [tree] into [BBBlock]s. [args] needs the resources and, for attachments, the user id.
 */
fun BBTree.toBlocks(args: BBArgs): List<BBBlock> = blocks(this, args + this.args)

private fun blocks(node: BBTree, args: BBArgs): List<BBBlock> {
    fun children(of: BBTree = node) = of.children.flatMap { blocks(it, args + it.args) }

    return when (val prototype = node.prototype) {
        TextPrototype -> listOf(BBBlock.Text(args.safeText))
        DividerPrototype -> listOf(BBBlock.Divider)
        ImagePrototype -> listOf(
            BBBlock.Image(firstText(node)?.toPrefixedUrlOrNull()?.proxyIfRequired(), ImagePrototype.width(args))
        )
        PdfPrototype -> listOf(BBBlock.Pdf(firstText(node)?.toPrefixedUrlOrNull(), PdfPrototype.width(args)))
        WikiPrototype -> firstText(node)?.takeIf { it.isNotBlank() }?.let { listOf(BBBlock.Wiki(it)) } ?: emptyList()
        SpoilerPrototype -> children().ifNotEmpty { listOf(BBBlock.Spoiler(SpoilerPrototype.title(args), merge(it))) }
        QuotePrototype -> listOf(BBBlock.Quote(QuotePrototype.quotedUser(args), merge(children())))
        CodePrototype -> children().ifNotEmpty { listOf(BBBlock.Code(merge(it))) }
        HidePrototype -> children().ifNotEmpty { listOf(BBBlock.Restricted(true, merge(it))) }
        AgeRestrictionPrototype -> children().ifNotEmpty { listOf(BBBlock.Restricted(false, merge(it))) }
        OrderedListPrototype, UnorderedListPrototype -> listOf(
            BBBlock.ListBlock(
                isOrdered = prototype == OrderedListPrototype,
                items = node.children
                    .filter { it.prototype == ListItemPrototype }
                    .map { merge(blocks(it, args + it.args)) }
            )
        )
        TablePrototype -> listOf(
            BBBlock.Table(
                node.children.filter { it.prototype == TableRowPrototype }.map { row ->
                    row.children.filter { it.prototype == TableCellPrototype }.map { cell ->
                        merge(blocks(cell, args + row.args + cell.args)).map { center(it) }
                    }
                }.filter { it.isNotEmpty() }
            )
        )
        AttachmentPrototype -> {
            val attachment = firstText(node)?.trim()

            when {
                attachment == null -> emptyList()
                AttachmentPrototype.isImage(attachment) -> listOf(
                    BBBlock.Image(AttachmentPrototype.constructUrl(args.safeUserId, attachment), null)
                )
                else -> mutateTexts(children(), AttachmentPrototype, args)
            }
        }
        CenterPrototype, LeftPrototype, RightPrototype -> {
            val alignment = when (prototype) {
                CenterPrototype -> BBBlock.Alignment.CENTER
                RightPrototype -> BBBlock.Alignment.END
                else -> BBBlock.Alignment.START
            }

            mutateTexts(children(), prototype as TextMutatorPrototype, args).map { block ->
                if (block is BBBlock.Text) block else BBBlock.Aligned(alignment, listOf(block))
            }
        }
        UrlPrototype -> mutateTexts(children(), UrlPrototype, args).map { block ->
            if (block is BBBlock.Text) block else BBBlock.Linked(UrlPrototype.url(args), listOf(block))
        }
        is TextMutatorPrototype -> mutateTexts(children(), prototype, args)
        else -> merge(children())
    }
}

/**
 * Applies a text style to all text in [blocks], like the View renderer did for all TextViews in the hierarchy.
 */
private fun mutateTexts(blocks: List<BBBlock>, prototype: TextMutatorPrototype, args: BBArgs): List<BBBlock> =
    blocks.map { block -> block.mapTexts { prototype.mutate(SpannableStringBuilder(it), args) } }

private fun BBBlock.mapTexts(transform: (CharSequence) -> CharSequence): BBBlock = when (this) {
    is BBBlock.Text -> BBBlock.Text(transform(text))
    is BBBlock.Spoiler -> copy(children = children.map { it.mapTexts(transform) })
    is BBBlock.Quote -> copy(children = children.map { it.mapTexts(transform) })
    is BBBlock.Code -> copy(children = children.map { it.mapTexts(transform) })
    is BBBlock.Restricted -> copy(children = children.map { it.mapTexts(transform) })
    is BBBlock.ListBlock -> copy(items = items.map { item -> item.map { it.mapTexts(transform) } })
    is BBBlock.Table -> copy(rows = rows.map { row -> row.map { cell -> cell.map { it.mapTexts(transform) } } })
    is BBBlock.Aligned -> copy(children = children.map { it.mapTexts(transform) })
    is BBBlock.Linked -> copy(children = children.map { it.mapTexts(transform) })
    else -> this
}

/**
 * Merges consecutive texts into one and removes blank texts, so paragraphs are rendered as one text.
 */
internal fun merge(blocks: List<BBBlock>): List<BBBlock> {
    val result = mutableListOf<BBBlock>()
    var currentText: SpannableStringBuilder? = null

    fun flush() {
        currentText?.let { text ->
            val trimmed = text.trimmed()

            if (trimmed.isNotBlank()) result += BBBlock.Text(trimmed)
        }

        currentText = null
    }

    blocks.forEach { block ->
        if (block is BBBlock.Text) {
            currentText = (currentText ?: SpannableStringBuilder()).append(block.text)
        } else {
            flush()

            result += block
        }
    }

    flush()

    return result
}

private fun SpannableStringBuilder.trimmed(): CharSequence {
    val start = indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 } ?: return ""
    val end = indexOfLast { !it.isWhitespace() } + 1

    return subSequence(start, end)
}

private fun center(block: BBBlock): BBBlock = when (block) {
    is BBBlock.Text -> BBBlock.Text(
        SpannableStringBuilder(block.text).apply {
            setSpan(
                AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
                0,
                length,
                Spannable.SPAN_INCLUSIVE_INCLUSIVE
            )
        }
    )
    else -> BBBlock.Aligned(BBBlock.Alignment.CENTER, listOf(block))
}

private fun firstText(node: BBTree): String? {
    val first = node.children.firstOrNull() ?: return null

    return when (first.prototype) {
        TextPrototype -> first.args.text?.toString()?.trim()
        else -> firstText(first)
    }
}

private inline fun List<BBBlock>.ifNotEmpty(block: (List<BBBlock>) -> List<BBBlock>) = if (isEmpty()) {
    this
} else {
    block(
        this
    )
}
