package me.proxer.app.ui.preview

import me.proxer.app.anime.AnimeStream
import me.proxer.app.anime.AnimeStreamInfo
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.media.comments.ParsedComment
import me.proxer.app.ui.view.bbcode.toBBTree
import me.proxer.library.entity.chat.ChatRoom
import me.proxer.library.entity.info.AdaptionInfo
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.EntrySeasonInfo
import me.proxer.library.entity.info.EntryTranslatorGroup
import me.proxer.library.entity.info.InfoGenre
import me.proxer.library.entity.info.InfoTag
import me.proxer.library.entity.info.MediaUserInfo
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.entity.info.Synonym
import me.proxer.library.entity.list.IndustryCore
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.entity.notifications.NewsArticle
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.CalendarDay
import me.proxer.library.enums.Category
import me.proxer.library.enums.Country
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.IndustryType
import me.proxer.library.enums.License
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.enums.MediaState
import me.proxer.library.enums.Medium
import me.proxer.library.enums.Season
import me.proxer.library.enums.SynonymType
import me.proxer.library.enums.UserMediaProgress
import org.threeten.bp.Instant
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Fake data for previews and screenshot tests.
 */
object PreviewData {

    private val now = Date(1_700_000_000_000L)

    private fun hoursAgo(hours: Long) = Date(now.time - TimeUnit.HOURS.toMillis(hours))

    val animeStreamInfo = AnimeStreamInfo(
        name = "Violet Evergarden",
        episodeAmount = 13,
        streams = listOf(
            AnimeStream(
                id = "1",
                hoster = "proxer-stream",
                hosterName = "Proxer-Stream",
                image = "proxer-stream.png",
                uploaderId = "1",
                uploaderName = "Ruby",
                date = Instant.ofEpochSecond(1_600_000_000),
                translatorGroupId = "2",
                translatorGroupName = "Gruppe Kirschblüte",
                isOfficial = false,
                isPublic = true,
                isSupported = true,
                resolutionResult = null
            ),
            AnimeStream(
                id = "2",
                hoster = "crunchyroll",
                hosterName = "Crunchyroll",
                image = "crunchyroll.png",
                uploaderId = "3",
                uploaderName = "Admin",
                date = Instant.ofEpochSecond(1_600_000_000),
                translatorGroupId = null,
                translatorGroupName = null,
                isOfficial = true,
                isPublic = false,
                isSupported = true,
                resolutionResult = null
            ),
            AnimeStream(
                id = "3",
                hoster = "other",
                hosterName = "Anderer Hoster",
                image = "other.png",
                uploaderId = "3",
                uploaderName = "Admin",
                date = Instant.ofEpochSecond(1_600_000_000),
                translatorGroupId = null,
                translatorGroupName = null,
                isOfficial = false,
                isPublic = true,
                isSupported = false,
                resolutionResult = StreamResolutionResult.Message("Dieser Stream ist auf https://proxer.me verfügbar.")
            )
        )
    )

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

    val entry = Entry(
        id = "1",
        name = "Sousou no Frieren",
        fskConstraints = setOf(FskConstraint.FSK_12, FskConstraint.VIOLENCE),
        description = "Die Elfe Frieren hat zusammen mit dem Helden Himmel den Dämonenkönig besiegt. Jahrzehnte " +
            "später macht sie sich auf eine neue Reise, um die Menschen besser zu verstehen. Eine ruhige Geschichte " +
            "über Vergänglichkeit, Freundschaft und Erinnerungen, die viele Zuschauer begeistert hat.",
        medium = Medium.ANIMESERIES,
        episodeAmount = 28,
        state = MediaState.FINISHED,
        ratingSum = 9_400,
        ratingAmount = 1_000,
        clicks = 100_000,
        category = Category.ANIME,
        license = License.LICENSED,
        adaptionInfo = AdaptionInfo("2", "Sousou no Frieren", Medium.MANGASERIES),
        isAgeRestricted = false,
        synonyms = listOf(
            Synonym("1", "1", SynonymType.ORIGINAL, "Sousou no Frieren"),
            Synonym("2", "1", SynonymType.ENGLISH, "Frieren: Beyond Journey's End")
        ),
        languages = setOf(MediaLanguage.GERMAN_SUB, MediaLanguage.ENGLISH_SUB),
        seasons = listOf(EntrySeasonInfo("1", 2023, Season.AUTUMN), EntrySeasonInfo("2", 2024, Season.WINTER)),
        translatorGroups = listOf(EntryTranslatorGroup("1", "Proxer Subs", Country.GERMANY)),
        industries = listOf(IndustryCore("1", "Madhouse", IndustryType.STUDIO, Country.JAPAN)),
        tags = listOf(
            InfoTag("1", "1", now, true, false, "Abenteuer", ""),
            InfoTag("2", "2", now, true, true, "Spoiler-Tag", ""),
            InfoTag("3", "3", now, false, false, "Unbewertet", "")
        ),
        genres = listOf(
            InfoGenre("1", "1", now, "Adventure", ""),
            InfoGenre("2", "2", now, "Drama", ""),
            InfoGenre("3", "3", now, "Fantasy", "")
        )
    )

    val mediaUserInfo = MediaUserInfo(
        isNoted = false,
        isFinished = true,
        isCanceled = false,
        isTopTen = true,
        isSubscribed = false
    )

    val comments = listOf(
        ParsedComment(
            id = "1",
            entryId = "1",
            authorId = "1",
            mediaProgress = UserMediaProgress.WATCHED,
            ratingDetails = RatingDetails(genre = 5, story = 4, animation = 5, characters = 4, music = 3),
            parsedContent = "Eine wunderschöne Serie mit [b]tollen[/b] Charakteren.".toBBTree(),
            overallRating = 9,
            episode = 28,
            helpfulVotes = 42,
            instant = Instant.ofEpochMilli(now.time),
            author = "Nutzer",
            image = ""
        )
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
