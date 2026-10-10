package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

object QuotePrototype : AutoClosingPrototype {

    private const val QUOTE_ARGUMENT = "quote"

    private val quoteAttributeRegex = Regex("quote *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *quote( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *quote *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val quote = BBUtils.cutAttribute(code, quoteAttributeRegex)

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(QUOTE_ARGUMENT to quote)))
    }

    /** The name of the quoted user, if any. */
    fun quotedUser(args: BBArgs) = args[QUOTE_ARGUMENT] as String?
}
