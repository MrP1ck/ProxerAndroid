package me.proxer.app.ui.view.bbcode.prototype

/**
 * @author Ruben Gees
 */
object RootPrototype : BBPrototype {

    @Suppress("RegExpUnexpectedAnchor")
    override val startRegex = Regex("x^")

    @Suppress("RegExpUnexpectedAnchor")
    override val endRegex = Regex("x^")
}
