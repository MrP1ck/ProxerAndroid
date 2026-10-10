package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object DividerPrototype : BBPrototype {

    override val startRegex = Regex(" *hr *", REGEX_OPTIONS)
    override val endRegex = Regex("/ *hr *", REGEX_OPTIONS)

    override val canHaveChildren get() = false
}
