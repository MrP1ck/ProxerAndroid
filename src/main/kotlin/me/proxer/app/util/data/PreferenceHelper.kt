package me.proxer.app.util.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.f2prateek.rx.preferences2.RxSharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.proxer.app.manga.MangaReaderOrientation
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.MainSection
import okhttp3.logging.HttpLoggingInterceptor
import org.threeten.bp.Instant

/**
 * @author Ruben Gees
 */
@Suppress("UseDataClass")
class PreferenceHelper(
    initializer: LocalDataInitializer,
    rxSharedPreferences: RxSharedPreferences,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        const val CAST_INTRODUCTORY_OVERLAY_SHOWN = "cast_introductory_overlay_shown"
        const val LAUNCHES = "launches"
        const val RATED = "rated"

        const val TWO_FACTOR_AUTHENTICATION = "two_factor_authentication"
        const val LAST_TAG_UPDATE_DATE = "last_tag_update_date"
        const val LAST_NEWS_DATE = "last_news_date"

        const val AGE_CONFIRMATION = "age_confirmation"
        const val LINK_CHECK = "check_links"
        const val AUTO_BOOKMARK = "auto_bookmark"
        const val CHECK_CELLULAR = "check_cellular"
        const val START_PAGE = "start_page"
        const val THEME = "theme"

        // Dynamic colors with the system light/dark setting. Falls back to the classic theme below Android 12.
        private const val DEFAULT_THEME = "3_2"
        const val NOTIFICATIONS_NEWS = "notifications_news"
        const val NOTIFICATIONS_ACCOUNT = "notifications_account"
        const val NOTIFICATIONS_CHAT = "notifications_chat"
        const val NOTIFICATIONS_INTERVAL = "notifications_interval"
        const val MANGA_READER_ORIENTATION = "manga_reader_orientation"
        const val EXTERNAL_CACHE = "external_cache"
        const val HTTP_LOG_LEVEL = "http_log_level"
        const val HTTP_VERBOSE = "http_log_verbose"
        const val HTTP_REDACT_TOKEN = "http_log_redact_token"
    }

    init {
        initializer.initAndMigrateIfNecessary()
    }

    var wasCastIntroductoryOverlayShown: Boolean
        get() = sharedPreferences.getBoolean(CAST_INTRODUCTORY_OVERLAY_SHOWN, false)
        set(value) {
            sharedPreferences.edit { putBoolean(CAST_INTRODUCTORY_OVERLAY_SHOWN, value) }
        }

    var launches: Int
        get() = sharedPreferences.getInt(LAUNCHES, 0)
        private set(value) {
            sharedPreferences.edit { putInt(LAUNCHES, value) }
        }

    var hasRated: Boolean
        get() = sharedPreferences.getBoolean(RATED, false)
        set(value) {
            sharedPreferences.edit { putBoolean(RATED, value) }
        }

    var isTwoFactorAuthenticationEnabled: Boolean
        get() = sharedPreferences.getBoolean(TWO_FACTOR_AUTHENTICATION, false)
        set(value) {
            sharedPreferences.edit { putBoolean(TWO_FACTOR_AUTHENTICATION, value) }
        }

    var lastTagUpdateDate: Instant
        get() = Instant.ofEpochMilli(sharedPreferences.getLong(LAST_TAG_UPDATE_DATE, 0L))
        set(value) {
            sharedPreferences.edit { putLong(LAST_TAG_UPDATE_DATE, value.toEpochMilli()) }
        }

    var lastNewsDate: Instant
        get() = Instant.ofEpochMilli(sharedPreferences.getLong(LAST_NEWS_DATE, 0L))
        set(value) {
            sharedPreferences.edit { putLong(LAST_NEWS_DATE, value.toEpochMilli()) }
        }

    var isAgeRestrictedMediaAllowed
        get() = sharedPreferences.getBoolean(AGE_CONFIRMATION, false)
        set(value) {
            sharedPreferences.edit { putBoolean(AGE_CONFIRMATION, value) }
        }

    var shouldCheckLinks
        get() = sharedPreferences.getBoolean(LINK_CHECK, true)
        set(value) {
            sharedPreferences.edit { putBoolean(LINK_CHECK, value) }
        }

    val isAgeRestrictedMediaAllowedObservable = rxSharedPreferences.getBoolean(AGE_CONFIRMATION, false)
        .asObservable()
        .skip(1)
        .distinctUntilChanged()
        .publish()
        .autoConnect()

    val areBookmarksAutomatic
        get() = sharedPreferences.getBoolean(AUTO_BOOKMARK, false)

    var shouldCheckCellular
        get() = sharedPreferences.getBoolean(CHECK_CELLULAR, true)
        set(value) {
            sharedPreferences.edit { putBoolean(CHECK_CELLULAR, value) }
        }

    val startPage
        get() = MainSection.fromIdOrDefault(
            sharedPreferences.getSafeString(START_PAGE, "0").toLongOrNull()
        )

    var areNewsNotificationsEnabled
        get() = sharedPreferences.getBoolean(NOTIFICATIONS_NEWS, false)
        set(value) {
            sharedPreferences.edit { putBoolean(NOTIFICATIONS_NEWS, value) }
        }

    var areAccountNotificationsEnabled
        get() = sharedPreferences.getBoolean(NOTIFICATIONS_ACCOUNT, false)
        set(value) {
            sharedPreferences.edit { putBoolean(NOTIFICATIONS_ACCOUNT, value) }
        }

    val areChatNotificationsEnabled
        get() = sharedPreferences.getBoolean(NOTIFICATIONS_CHAT, true)

    val notificationsInterval
        get() = sharedPreferences.getSafeString(NOTIFICATIONS_INTERVAL, "30").toLong()

    var mangaReaderOrientation
        get() = MangaReaderOrientation.values()[
            sharedPreferences.getInt(MANGA_READER_ORIENTATION, MangaReaderOrientation.VERTICAL.ordinal)
        ]
        set(value) {
            sharedPreferences.edit { putInt(MANGA_READER_ORIENTATION, value.ordinal) }
        }

    var shouldCacheExternally
        get() = sharedPreferences.getBoolean(EXTERNAL_CACHE, true)
        set(value) {
            sharedPreferences.edit { putBoolean(EXTERNAL_CACHE, value) }
        }

    val isCacheExternallySet
        get() = sharedPreferences.contains(EXTERNAL_CACHE)

    var themeContainer
        get() = ThemeContainer.fromPreferenceString(sharedPreferences.getSafeString(THEME, DEFAULT_THEME))
        set(value) {
            sharedPreferences.edit { putString(THEME, value.toPreferenceString()) }

            mutableThemeFlow.value = value
        }

    private val mutableThemeFlow = MutableStateFlow(themeContainer)

    /**
     * The current theme. Unlike [themeObservable], this emits the current value immediately, which is what Compose
     * needs to switch the theme without recreating the Activity.
     */
    val themeFlow: StateFlow<ThemeContainer> = mutableThemeFlow.asStateFlow()

    val themeObservable = rxSharedPreferences.getString(THEME, DEFAULT_THEME)
        .asObservable()
        .skip(1)
        .map { ThemeContainer.fromPreferenceString(it) }
        .distinctUntilChanged()
        .publish()
        .autoConnect()

    val httpLogLevel
        get() = when (sharedPreferences.getString(HTTP_LOG_LEVEL, "0")) {
            "0" -> HttpLoggingInterceptor.Level.BASIC
            "1" -> HttpLoggingInterceptor.Level.HEADERS
            "2" -> HttpLoggingInterceptor.Level.BODY
            else -> error("Unknown http log level saved in shared preferences")
        }

    val shouldLogHttpVerbose
        get() = sharedPreferences.getBoolean(HTTP_VERBOSE, false)

    val shouldRedactToken
        get() = sharedPreferences.getBoolean(HTTP_REDACT_TOKEN, false)

    fun incrementLaunches() = sharedPreferences.edit { putInt(LAUNCHES, sharedPreferences.getInt(LAUNCHES, 0) + 1) }
}
