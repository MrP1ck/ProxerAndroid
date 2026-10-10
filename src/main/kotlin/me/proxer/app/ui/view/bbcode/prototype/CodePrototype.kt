package me.proxer.app.ui.view.bbcode.prototype

/**
 * @author Ruben Gees
 */
object CodePrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *code( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *code *", BBPrototype.REGEX_OPTIONS)
}
