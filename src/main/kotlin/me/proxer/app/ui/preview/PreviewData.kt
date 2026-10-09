package me.proxer.app.ui.preview

import me.proxer.library.entity.chat.ChatRoom
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.entity.notifications.NewsArticle
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.CalendarDay
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.Medium
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Fake data for previews and screenshot tests.
 */
object PreviewData {

    private val now = Date(1_700_000_000_000L)

    private fun hoursAgo(hours: Long) = Date(now.time - TimeUnit.HOURS.toMillis(hours))

    val newsArticles = listOf(
        NewsArticle(
            id = "1",
            date = hoursAgo(2),
            description = "Die zweite Staffel startet im Oktober. Ein neuer Trailer stellt die Charaktere vor, " +
                "die in der Fortsetzung eine größere Rolle spielen werden. Außerdem wurde das Opening angekündigt.",
            image = "",
            subject = "Neuer Trailer zur zweiten Staffel veröffentlicht",
            threadId = "1",
            authorId = "1",
            author = "Proxer",
            categoryId = "1",
            category = "Anime"
        ),
        NewsArticle(
            id = "2",
            date = hoursAgo(26),
            description = "Der Manga erhält eine Anime-Adaption.",
            image = "",
            subject = "Anime-Adaption angekündigt",
            threadId = "2",
            authorId = "1",
            author = "Proxer",
            categoryId = "2",
            category = "Manga"
        )
    )

    val mediaListEntries = listOf(
        mediaListEntry("1", "Shingeki no Kyojin", Medium.ANIMESERIES, 25, MediaState.FINISHED, 9.1f),
        mediaListEntry("2", "Sousou no Frieren", Medium.ANIMESERIES, 28, MediaState.AIRING, 9.4f),
        mediaListEntry("3", "Kimi no Na wa.", Medium.MOVIE, 1, MediaState.FINISHED, 8.8f),
        mediaListEntry("4", "Ein sehr langer Titel, der nicht in eine Zeile passt", Medium.OVA, 2, MediaState.FINISHED, 0f)
    )

    val bookmarks = listOf(
        Bookmark(
            id = "1",
            entryId = "1",
            category = Category.ANIME,
            name = "Sousou no Frieren",
            episode = 12,
            language = MediaLanguage.GERMAN_SUB,
            medium = Medium.ANIMESERIES,
            state = MediaState.AIRING,
            chapterName = null,
            isAvailable = true
        ),
        Bookmark(
            id = "2",
            entryId = "2",
            category = Category.MANGA,
            name = "One Piece",
            episode = 1100,
            language = MediaLanguage.ENGLISH,
            medium = Medium.MANGASERIES,
            state = MediaState.AIRING,
            chapterName = "Ein neues Kapitel",
            isAvailable = false
        )
    )

    val calendarEntries = listOf(
        calendarEntry("1", "Sousou no Frieren", 13, CalendarDay.MONDAY, 2),
        calendarEntry("2", "Jujutsu Kaisen", 7, CalendarDay.MONDAY, -3),
        calendarEntry("3", "One Piece", 1100, CalendarDay.TUESDAY, -30)
    )

    val chatRooms = listOf(
        ChatRoom("1", "Allgemein", "Hier wird über alles geredet. Regeln: https://proxer.me/wiki", false),
        ChatRoom("2", "Ankündigungen", "", true)
    )

    private fun mediaListEntry(
        id: String,
        name: String,
        medium: Medium,
        episodeAmount: Int,
        state: MediaState,
        rating: Float
    ) = MediaListEntry(
        id = id,
        name = name,
        genres = setOf("Action", "Drama"),
        medium = medium,
        episodeAmount = episodeAmount,
        state = state,
        ratingSum = (rating * 10).toInt(),
        ratingAmount = if (rating > 0) 10 else 0,
        languages = setOf(MediaLanguage.GERMAN_SUB, MediaLanguage.ENGLISH_SUB)
    )

    private fun calendarEntry(id: String, name: String, episode: Int, day: CalendarDay, hoursFromNow: Long) =
        CalendarEntry(
            id = id,
            entryId = id,
            name = name,
            episode = episode,
            episodeTitle = "",
            date = hoursAgo(-hoursFromNow),
            timezone = "Europe/Berlin",
            industryId = "1",
            industryName = "Crunchyroll",
            weekDay = day,
            uploadDate = hoursAgo(-hoursFromNow),
            genres = setOf("Action"),
            ratingSum = 90,
            ratingAmount = 10
        )
}
