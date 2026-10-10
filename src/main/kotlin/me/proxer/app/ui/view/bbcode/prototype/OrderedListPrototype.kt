package me.proxer.app.ui.view.bbcode.prototype

/**
 * @author Ruben Gees
 */
object OrderedListPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *ol( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *ol *", BBPrototype.REGEX_OPTIONS)
}
