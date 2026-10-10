package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object HidePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *hide( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *hide *", REGEX_OPTIONS)
}
