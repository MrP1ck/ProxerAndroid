package me.proxer.app.ui.view.bbcode.prototype

import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object AgeRestrictionPrototype : AutoClosingPrototype {

    override val startRegex = Regex(" *age18( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *age18 *", REGEX_OPTIONS)
}
