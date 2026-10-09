package me.proxer.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import me.proxer.app.TestApplication
import me.proxer.app.comment.CommentCard
import me.proxer.app.comment.CommentCardData
import me.proxer.app.media.info.MediaInfoActions
import me.proxer.app.media.info.MediaInfoContent
import me.proxer.app.ui.preview.PreviewData
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The tabs of the media screen with fake data, in light and dark mode.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestApplication::class, qualifiers = "w411dp-h1600dp-xxhdpi")
class MediaScreensScreenshotTest(private val isDark: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun parameters() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mediaInfo() = composeRule.captureScreen("screens", "media_info", isDark) {
        MediaInfoContent(PreviewData.entry, PreviewData.mediaUserInfo, true, MediaInfoActions(), PaddingValues())
    }

    // The BBCode renderer is still View based and initializes Glide, which needs Koin. Enabled with the Compose renderer.
    @Ignore("Needs the Compose BBCode renderer")
    @Test
    fun comment() = composeRule.captureScreen("screens", "comment", isDark) {
        val comment = PreviewData.comments.first()

        CommentCard(
            comment = CommentCardData(
                id = comment.id,
                title = comment.author,
                imageUrl = null,
                subtitle = "Gesehen",
                time = "Vor 2 Tagen",
                overallRating = comment.overallRating,
                ratingDetails = comment.ratingDetails,
                content = comment.parsedContent,
                authorId = comment.authorId,
                helpfulVotes = comment.helpfulVotes,
                isOwn = true
            ),
            onHeaderClick = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
