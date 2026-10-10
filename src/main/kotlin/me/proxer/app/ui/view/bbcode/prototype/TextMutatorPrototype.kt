package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import me.proxer.app.ui.view.bbcode.BBArgs

/**
 * @author Ruben Gees
 */
interface TextMutatorPrototype : BBPrototype {

    fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder
}
