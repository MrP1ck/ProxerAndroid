package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object TableRowPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *tr( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *tr *", REGEX_OPTIONS)
}
