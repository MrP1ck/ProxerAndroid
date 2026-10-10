package me.proxer.app.ui.view.bbcode.prototype

/**
 * @author Ruben Gees
 */
object UnorderedListPrototype : BBPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *ul( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ul *", BBPrototype.REGEX_OPTIONS)
}
