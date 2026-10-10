package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object SpoilerPrototype : AutoClosingPrototype {

    const val SPOILER_TEXT_COLOR_ARGUMENT = "spoiler_text_color"
    const val SPOILER_EXPAND_ARGUMENT = "spoiler_expand"

    private const val TITLE_ARGUMENT = "title"

    private val attributeRegex = Regex("spoiler *= *(.+?)$", REGEX_OPTIONS)

    override val startRegex = Regex(" *spoiler( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *spoiler *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val title = BBUtils.cutAttribute(code, attributeRegex)

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(TITLE_ARGUMENT to title)))
    }

    /** The title of the spoiler, if any. */
    fun title(args: BBArgs) = args[TITLE_ARGUMENT] as String?
}
