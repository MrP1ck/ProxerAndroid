package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.BBUtils
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * @author Ruben Gees
 */
object UrlPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    private const val URL_ARGUMENT = "url"

    private val attributeRegex = Regex("url *= *(.+?)( |$)", REGEX_OPTIONS)
    private val invalidUrl = "https://proxer.me/404".toHttpUrl()

    override val startRegex = Regex(" *url *= *.+?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *url *", REGEX_OPTIONS)

    override fun construct(code: String, parent: BBTree): BBTree {
        val url = BBUtils.cutAttribute(code, attributeRegex)?.trim() ?: ""
        val parsedUrl = url.toPrefixedUrlOrNull() ?: invalidUrl

        return BBTree(this, parent, args = BBArgs(custom = arrayOf(URL_ARGUMENT to parsedUrl)))
    }

    /** The target of the link. */
    fun url(args: BBArgs) = args[URL_ARGUMENT] as HttpUrl

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val url = args[URL_ARGUMENT] as HttpUrl

        return text.linkifyUrl(url)
    }
}
