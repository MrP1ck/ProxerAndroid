package me.proxer.app.ui.view.bbcode.prototype

/**
 * An article of the Proxer wiki, which is embedded (see BBEmbeds).
 */
object WikiPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *wiki( .*?)?", BBPrototype.REGEX_OPTIONS)
    override val endRegex = Regex("/ *wiki *", BBPrototype.REGEX_OPTIONS)
}
