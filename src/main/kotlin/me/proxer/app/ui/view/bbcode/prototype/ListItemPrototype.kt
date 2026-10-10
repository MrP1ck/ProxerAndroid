package me.proxer.app.ui.view.bbcode.prototype

/**
 * @author Ruben Gees
 */
object ListItemPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *li( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *li *", BBPrototype.REGEX_OPTIONS)
}
