package me.proxer.app.profile

import android.app.Activity
import android.content.Intent
import android.widget.ImageView
import androidx.compose.runtime.Composable
import me.proxer.app.base.ComposeActivity
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.intentFor

/**
 * Hosts the [ProfileScreen] of a user. Also handles the deep links to profiles.
 *
 * @author Ruben Gees
 */
class ProfileActivity : ComposeActivity() {

    companion object {
        private const val USER_ID_EXTRA = "user_id"
        private const val USERNAME_EXTRA = "username"
        private const val IMAGE_ID_EXTRA = "image_id"

        private const val ABOUT_SUB_SECTION = "about"
        private const val ANIME_SUB_SECTION = "anime"
        private const val MANGA_SUB_SECTION = "manga"
        private const val HISTORY_SUB_SECTION = "chronik"

        @Suppress("UNUSED_PARAMETER")
        fun navigateTo(
            context: Activity,
            userId: String? = null,
            username: String? = null,
            image: String? = null,
            imageView: ImageView? = null
        ) {
            if (userId.isNullOrBlank() && username.isNullOrBlank()) {
                return
            }

            context.intentFor<ProfileActivity>(
                USER_ID_EXTRA to userId,
                USERNAME_EXTRA to username,
                IMAGE_ID_EXTRA to image
            ).let { ActivityUtils.navigateToWithImageTransition(it, context, null) }
        }
    }

    private val userId: String?
        get() = when (intent.hasExtra(USER_ID_EXTRA)) {
            true -> intent.getStringExtra(USER_ID_EXTRA)
            false -> intent.data?.pathSegments?.getOrNull(1)
        }

    private val initialTab: ProfileTab
        get() = when (intent.action) {
            Intent.ACTION_VIEW -> when (intent.data?.pathSegments?.getOrNull(2)) {
                ABOUT_SUB_SECTION -> ProfileTab.ABOUT
                ANIME_SUB_SECTION -> ProfileTab.ANIME
                MANGA_SUB_SECTION -> ProfileTab.MANGA
                HISTORY_SUB_SECTION -> ProfileTab.HISTORY
                else -> ProfileTab.INFO
            }
            else -> ProfileTab.INFO
        }

    @Composable
    override fun Content() {
        ProfileScreen(
            userId = userId,
            username = intent.getStringExtra(USERNAME_EXTRA),
            initialImage = intent.getStringExtra(IMAGE_ID_EXTRA),
            initialTab = initialTab
        )
    }
}
