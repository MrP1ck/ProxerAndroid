package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object ImagePrototype : AutoClosingPrototype {

    private const val WIDTH_ARGUMENT = "width"

    private val widthAttributeRegex = Regex("(?:size)? *= *(.+?)( |$)", REGEX_OPTIONS)

    override val startRegex = Regex(" *img *=? *\"?.*?\"?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *img *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val width = BBUtils.cutAttribute(code, widthAttributeRegex)?.toIntOrNull()

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(WIDTH_ARGUMENT to width)))
    }

    /** The width in pixels set with the size attribute, if any. */
    fun width(args: BBArgs) = args[WIDTH_ARGUMENT] as Int?
}
