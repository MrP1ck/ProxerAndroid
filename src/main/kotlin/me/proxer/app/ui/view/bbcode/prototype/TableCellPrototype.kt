package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object TableCellPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *td( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *td *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree) = BBTree(this, parent)
}
