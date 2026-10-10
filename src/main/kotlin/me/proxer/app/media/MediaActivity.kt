package me.proxer.app.media

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.ImageView
import androidx.compose.runtime.Composable
import me.proxer.app.base.ComposeActivity
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.intentFor
import me.proxer.library.enums.Category

/**
 * Hosts the [MediaScreen] of an anime, manga or novel. Also handles the deep links to entries.
 *
 * @author Ruben Gees
 */
class MediaActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"
        private const val CATEGORY_EXTRA = "category"

        private const val COMMENTS_SUB_SECTION = "comments"
        private const val EPISODES_SUB_SECTION = "episodes"
        private const val EPISODES_ALTERNATIVE_SUB_SECTION = "list"
        private const val RELATIONS_SUB_SECTION = "relation"
        private const val RECOMMENDATIONS_SUB_SECTION = "recommendations"
        private const val DISCUSSIONS_SUB_SECTION = "forum"

        @Suppress("UNUSED_PARAMETER")
        fun navigateTo(
            context: Activity,
            id: String,
            name: String? = null,
            category: Category? = null,
            imageView: ImageView? = null
        ) {
            ActivityUtils.navigateToWithImageTransition(getIntent(context, id, name, category), context, null)
        }

        fun getIntent(
            context: Context,
            id: String,
            name: String? = null,
            category: Category? = null
        ): Intent {
            return context.intentFor<MediaActivity>(
                ID_EXTRA to id,
                NAME_EXTRA to name,
                CATEGORY_EXTRA to category
            )
        }
    }

    private val id: String
        get() = when (intent.hasExtra(ID_EXTRA)) {
            true -> intent.getSafeStringExtra(ID_EXTRA)
            false -> intent.data?.pathSegments?.getOrNull(1) ?: "-1"
        }

    private val initialTab: MediaTab
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data?.pathSegments?.getOrNull(2)) {
                COMMENTS_SUB_SECTION -> MediaTab.COMMENTS
                EPISODES_SUB_SECTION, EPISODES_ALTERNATIVE_SUB_SECTION -> MediaTab.EPISODES
                RELATIONS_SUB_SECTION -> MediaTab.RELATIONS
                RECOMMENDATIONS_SUB_SECTION -> MediaTab.RECOMMENDATIONS
                DISCUSSIONS_SUB_SECTION -> MediaTab.DISCUSSIONS
                else -> MediaTab.INFO
            }
            else -> MediaTab.INFO
        }

    @Composable
    override fun Content() {
        MediaScreen(
            entryId = id,
            initialName = intent.getStringExtra(NAME_EXTRA),
            initialCategory = intent.getSerializableExtra(CATEGORY_EXTRA) as Category?,
            initialTab = initialTab
        )
    }
}
