package me.proxer.app.ui.view.bbcode.prototype

import android.text.Layout.Alignment.ALIGN_CENTER
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import androidx.core.text.set
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS

/**
 * @author Ruben Gees
 */
object CenterPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    override val startRegex = Regex(" *center( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *center *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs) = text.apply {
        this[0..length] = AlignmentSpan.Standard(ALIGN_CENTER)
    }
}
