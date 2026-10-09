package me.proxer.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import me.proxer.app.TestApplication
import me.proxer.app.anime.schedule.ScheduleContent
import me.proxer.app.bookmark.BookmarkContent
import me.proxer.app.chat.ChatRoomList
import me.proxer.app.media.list.MediaListContent
import me.proxer.app.media.list.MediaListFilter
import me.proxer.app.news.NewsContent
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.preview.PreviewData
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The content of the top level screens with fake data, in light and dark mode.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = TestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi")
class TopLevelScreensScreenshotTest(private val isDark: Boolean) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun parameters() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule
    val composeRule = createComposeRule()

    private val noPadding = PaddingValues()

    @Test
    fun news() = composeRule.captureScreen("screens", "news", isDark) {
        NewsContent(ContentState(PreviewData.newsArticles), {}, {}, {}, {}, {}, noPadding)
    }

    @Test
    fun mediaList() = composeRule.captureScreen("screens", "media_list", isDark) {
        MediaListContent(
            category = Category.ANIME,
            state = ContentState(PreviewData.mediaListEntries),
            filter = MediaListFilter(MediaType.ALL_ANIME, hideFinished = true),
            onTypeChange = {},
            onFilterClick = {},
            onLoadMore = {},
            onRefresh = {},
            onErrorAction = {},
            onEntryClick = {},
            contentPadding = noPadding
        )
    }

    @Test
    fun bookmarks() = composeRule.captureScreen("screens", "bookmarks", isDark) {
        BookmarkContent(
            ContentState(PreviewData.bookmarks), null, false, false, {}, {}, {}, {}, {}, {}, {}, {}, noPadding
        )
    }

    @Test
    fun schedule() = composeRule.captureScreen("screens", "schedule", isDark) {
        ScheduleContent(ContentState(PreviewData.calendarEntries.groupBy { it.weekDay }), {}, {}, {}, noPadding)
    }

    @Test
    fun chatRooms() = composeRule.captureScreen("screens", "chat_rooms", isDark) {
        ChatRoomList(ContentState(PreviewData.chatRooms), {}, {}, {}, noPadding)
    }
}
