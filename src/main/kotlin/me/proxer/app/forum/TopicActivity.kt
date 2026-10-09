package me.proxer.app.forum

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import me.proxer.app.base.ComposeActivity
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.startActivity

/**
 * Hosts the [TopicScreen] of a forum thread. Also handles the deep links to threads.
 *
 * @author Ruben Gees
 */
class TopicActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val CATEGORY_ID_EXTRA = "category_id"
        private const val TOPIC_EXTRA = "topic"

        private const val TOUZAI_PATH = "/touzai"
        private const val TOUZAI_CATEGORY = "310"

        fun navigateTo(context: Activity, id: String, categoryId: String, topic: String? = null) {
            context.startActivity<TopicActivity>(
                ID_EXTRA to id,
                CATEGORY_ID_EXTRA to categoryId,
                TOPIC_EXTRA to topic
            )
        }

        fun getIntent(context: Context, id: String, categoryId: String, topic: String? = null): Intent {
            return context.intentFor<TopicActivity>(
                ID_EXTRA to id,
                CATEGORY_ID_EXTRA to categoryId,
                TOPIC_EXTRA to topic
            )
        }
    }

    private val id: String
        get() = when (intent.hasExtra(ID_EXTRA)) {
            true -> intent.getSafeStringExtra(ID_EXTRA)
            false -> when (intent.data?.path == TOUZAI_PATH) {
                true -> intent.data?.getQueryParameter("id") ?: "-1"
                else -> intent.data?.pathSegments?.getOrNull(2) ?: "-1"
            }
        }

    private val categoryId: String
        get() = when (intent.hasExtra(CATEGORY_ID_EXTRA)) {
            true -> intent.getSafeStringExtra(CATEGORY_ID_EXTRA)
            false -> when (intent.data?.path == TOUZAI_PATH) {
                true -> TOUZAI_CATEGORY
                else -> intent.data?.pathSegments?.getOrNull(1) ?: "-1"
            }
        }

    @Composable
    override fun Content() {
        TopicScreen(
            id = id,
            categoryId = categoryId,
            initialTopic = intent.getStringExtra(TOPIC_EXTRA),
            deepLinkUrl = if (intent.action == Intent.ACTION_VIEW) intent.dataString else null
        )
    }
}
