package me.proxer.app.ui.view.bbcode.prototype

import android.text.SpannableStringBuilder
import me.proxer.app.R
import me.proxer.app.ui.view.bbcode.BBArgs
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.ui.view.bbcode.linkifyUrl
import me.proxer.app.ui.view.bbcode.prototype.BBPrototype.Companion.REGEX_OPTIONS
import me.proxer.app.ui.view.bbcode.toSpannableStringBuilder
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
object AttachmentPrototype : ConditionalTextMutatorPrototype, AutoClosingPrototype {

    private val imageExtensions = arrayOf("png", "jpg", "jpeg", "gif")
    private val whitespaceRegex = Regex("\\s")

    override val startRegex = Regex(" *attachment( *=\"?.+?\"?)?( .*?)?", REGEX_OPTIONS)
    override val endRegex = Regex("/ *attachment *", REGEX_OPTIONS)

    override fun mutate(text: SpannableStringBuilder, args: BBArgs): SpannableStringBuilder {
        val url = constructUrl(args.safeUserId, text)

        return text.toSpannableStringBuilder()
            .replace(0, text.length, args.safeResources.getString(R.string.view_bbcode_attachment_link))
            .linkifyUrl(url)
    }

    override fun canOptimize(recursiveChildren: List<BBTree>): Boolean {
        val firstChild = recursiveChildren.firstOrNull()

        return if (firstChild?.prototype === TextPrototype) {
            !isImage(firstChild.args.safeText) && super.canOptimize(recursiveChildren)
        } else {
            false
        }
    }

    fun isImage(attachment: CharSequence) =
        imageExtensions.any { attachment.endsWith(it, true) }

    fun constructUrl(userId: String, attachment: CharSequence) = ProxerUrls.webBase.newBuilder()
        .addPathSegments("media/kunena/attachments/$userId/${attachment.replace(whitespaceRegex, "")}")
        .build()
}
