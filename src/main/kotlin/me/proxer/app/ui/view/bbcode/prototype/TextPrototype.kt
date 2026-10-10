package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.app.util.extension.linkify

/**
 * Plain text. Links and mentions in it are turned into spans.
 *
 * @author Ruben Gees
 */
object TextPrototype : BBPrototype, ContentPrototype {

    @Suppress("RegExpUnexpectedAnchor")
    override val startRegex = Regex("x^")

    @Suppress("RegExpUnexpectedAnchor")
    override val endRegex = Regex("x^")

    override fun construct(code: String, parent: BBTree): BBTree {
        return BBTree(this, parent, args = BBArgs(text = code.toSpannableStringBuilder().linkify()))
    }

    override fun isBlank(args: BBArgs): Boolean {
        return args.text?.isBlank() != false
    }
}
