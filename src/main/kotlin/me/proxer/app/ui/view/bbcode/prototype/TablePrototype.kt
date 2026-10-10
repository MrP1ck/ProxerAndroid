package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object TablePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *table( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *table *", REGEX_OPTIONS)
}
