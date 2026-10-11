package me.proxer.app

import android.content.Context
import android.content.res.Resources
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.WorkManager
import com.squareup.moshi.Moshi
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.anime.AnimeViewModel
import me.proxer.app.anime.schedule.ScheduleViewModel
import me.proxer.app.auth.LoginHandler
import me.proxer.app.auth.LoginViewModel
import me.proxer.app.auth.LogoutViewModel
import me.proxer.app.auth.ProxerLoginTokenManager
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.bookmark.BookmarkViewModel
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.conference.ConferenceViewModel
import me.proxer.app.chat.prv.conference.info.ConferenceInfoViewModel
import me.proxer.app.chat.prv.create.CreateConferenceViewModel
import me.proxer.app.chat.prv.message.MessengerReportViewModel
import me.proxer.app.chat.prv.message.MessengerViewModel
import me.proxer.app.chat.prv.sync.MessengerDatabase
import me.proxer.app.comment.EditCommentViewModel
import me.proxer.app.forum.TopicViewModel
import me.proxer.app.info.character.CharacterViewModel
import me.proxer.app.info.industry.IndustryInfoViewModel
import me.proxer.app.info.industry.IndustryProjectViewModel
import me.proxer.app.info.person.PersonViewModel
import me.proxer.app.info.translatorgroup.TranslatorGroupInfoViewModel
import me.proxer.app.info.translatorgroup.TranslatorGroupProjectViewModel
import me.proxer.app.manga.MangaViewModel
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.media.TagDatabase
import me.proxer.app.media.character.EntryCharactersViewModel
import me.proxer.app.media.comments.CommentsViewModel
import me.proxer.app.media.discussion.DiscussionViewModel
import me.proxer.app.media.episode.EpisodeViewModel
import me.proxer.app.media.list.MediaListFilter
import me.proxer.app.media.list.MediaListViewModel
import me.proxer.app.media.person.EntryPersonsViewModel
import me.proxer.app.media.recommendation.RecommendationViewModel
import me.proxer.app.media.relation.RelationViewModel
import me.proxer.app.news.NewsViewModel
import me.proxer.app.notification.NotificationViewModel
import me.proxer.app.profile.ProfileViewModel
import me.proxer.app.profile.about.ProfileAboutViewModel
import me.proxer.app.profile.comment.ProfileCommentViewModel
import me.proxer.app.profile.history.HistoryViewModel
import me.proxer.app.profile.media.ProfileMediaListViewModel
import me.proxer.app.profile.settings.ProfileSettingsViewModel
import me.proxer.app.profile.topten.TopTenViewModel
import me.proxer.app.ui.LinkCheckViewModel
import me.proxer.app.util.Validators
import me.proxer.app.util.data.EventBus
import me.proxer.app.util.data.InstantJsonAdapter
import me.proxer.app.util.data.LocalDataInitializer
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.http.CacheInterceptor
import me.proxer.app.util.http.ConnectionCloseInterceptor
import me.proxer.app.util.http.ConnectivityInterceptor
import me.proxer.app.util.http.HttpsUpgradeInterceptor
import me.proxer.app.util.http.TaggedSocketFactory
import me.proxer.app.util.http.UserAgentInterceptor
import me.proxer.app.util.logging.HttpTimberLogger
import me.proxer.library.LoginTokenManager
import me.proxer.library.ProxerApi
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentSortCriteria
import me.proxer.library.enums.Language
import okhttp3.Cache
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File
import java.security.KeyStore
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

const val DEFAULT_PREFERENCES = "defaultPreferences"
private const val STORAGE_PREFERENCES = "storagePreferences"

private const val STORAGE_PREFERENCES_NAME = "me.proxer.encrypted_preferences"

private const val CHAT_DATABASE_NAME = "chat.db"
private const val TAG_DATABASE_NAME = "tag.db"

private const val HTTP_CACHE_SIZE = 1_024L * 1_024L * 10L
private const val HTTP_CACHE_NAME = "http"

private const val API_TOKEN_HEADER = "proxer-api-token"

private val headersToRedact = listOf("proxer-api-key", "set-cookie")

/** Preferences, databases and the helpers on top of them. */
private val storageModule = module {
    single { androidContext().packageManager }

    single(named(DEFAULT_PREFERENCES)) {
        // The file of PreferenceManager.getDefaultSharedPreferences, which the settings used before.
        androidContext().getSharedPreferences("${androidContext().packageName}_preferences", Context.MODE_PRIVATE)
    }
    single(named(STORAGE_PREFERENCES)) {
        val masterKey = MasterKey.Builder(androidContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            androidContext(),
            STORAGE_PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    single { PreferenceHelper(get(), get(named(DEFAULT_PREFERENCES))) }
    single { StorageHelper(get(), get(named(STORAGE_PREFERENCES)), get()) }

    single { EventBus() }

    single {
        Room.databaseBuilder(androidContext(), MessengerDatabase::class.java, CHAT_DATABASE_NAME)
            .addMigrations(MessengerDatabase.MIGRATION_1_2)
            .build()
    }

    single {
        Room.databaseBuilder(androidContext(), TagDatabase::class.java, TAG_DATABASE_NAME)
            .addMigrations(TagDatabase.MIGRATION_1_2)
            .build()
    }

    single { get<MessengerDatabase>().dao() }
    single { get<TagDatabase>().dao() }

    single {
        LocalDataInitializer(
            androidContext(),
            get(named(DEFAULT_PREFERENCES)),
            get(named(STORAGE_PREFERENCES))
        )
    }
}

/** The HTTP stack and the Proxer API. */
private val networkModule = module {
    single {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(null as KeyStore?)
        }

        trustManagerFactory.trustManagers.filterIsInstance(X509TrustManager::class.java).first()
    }

    single {
        val preferenceHelper = get<PreferenceHelper>()

        val loggingInterceptor = when {
            BuildConfig.LOG -> HttpLoggingInterceptor(HttpTimberLogger()).apply {
                level = preferenceHelper.httpLogLevel

                headersToRedact.forEach { redactHeader(it) }

                if (preferenceHelper.shouldRedactToken) {
                    redactHeader(API_TOKEN_HEADER)
                }
            }
            else -> null
        }

        OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT))
            .socketFactory(TaggedSocketFactory())
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor(CacheInterceptor())
            .addInterceptor(ConnectivityInterceptor(get()))
            .addInterceptor(HttpsUpgradeInterceptor())
            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(ConnectionCloseInterceptor())
            .addInterceptor(BrotliInterceptor)
            .cache(Cache(File(androidContext().cacheDir, HTTP_CACHE_NAME), HTTP_CACHE_SIZE))
            .apply {
                if (loggingInterceptor != null) {
                    if (preferenceHelper.shouldLogHttpVerbose) {
                        addNetworkInterceptor(loggingInterceptor)
                    } else {
                        addInterceptor(loggingInterceptor)
                    }
                }
            }
            .build()
    }

    single {
        Moshi.Builder()
            .add(Instant::class.java, InstantJsonAdapter())
            .build()
    }

    single {
        ProxerApi.Builder(BuildConfig.PROXER_API_KEY)
            .enableRateLimitProtection()
            .loginTokenManager(get())
            .userAgent(USER_AGENT)
            .client(get())
            .moshi(get())
            .build()
    }

    single<LoginTokenManager> { ProxerLoginTokenManager(get()) }
}

/** Everything else the screens and workers need. */
private val appModule = module {
    single { WorkManager.getInstance(androidContext()) }

    single { Validators(get(), get()) }
    single { ViewModelDependencies(get(), get(), get(), get(), get()) }

    single { LoginHandler(get(), get(), get()) }
}

private val viewModelModule = module {
    viewModel { LoginViewModel(get(), get(), get()) }
    viewModel { LogoutViewModel(get()) }

    viewModel { NewsViewModel(get()) }
    viewModel { NotificationViewModel(get()) }

    viewModel { CreateConferenceViewModel(get(), get(), get()) }
    viewModel { (searchQuery: String) -> ConferenceViewModel(get(), get(), searchQuery) }
    viewModel { (conferenceId: String) -> ConferenceInfoViewModel(get(), conferenceId) }
    viewModel { (initialConference: LocalConference) -> MessengerViewModel(get(), get(), initialConference) }
    viewModel { MessengerReportViewModel(get(), get(), get()) }

    viewModel { (searchQuery: String?, category: Category, filterAvailable: Boolean) ->
        BookmarkViewModel(get(), searchQuery, category, filterAvailable)
    }

    viewModel { (initialFilter: MediaListFilter) -> MediaListViewModel(get(), initialFilter, get()) }

    viewModel { ScheduleViewModel(get()) }

    viewModel { (userId: String?, username: String?) -> ProfileAboutViewModel(get(), userId, username) }
    viewModel { (userId: String?, username: String?) -> ProfileViewModel(get(), userId, username) }
    viewModel { (userId: String?, username: String?) -> TopTenViewModel(get(), userId, username) }
    viewModel { (userId: String?, username: String?) -> HistoryViewModel(get(), userId, username) }
    viewModel { ProfileSettingsViewModel(get(), get()) }

    viewModel { parameterList ->
        ProfileMediaListViewModel(get(), parameterList[0], parameterList[1], parameterList[2], parameterList[3])
    }

    viewModel { (userId: String?, username: String?, category: Category?) ->
        ProfileCommentViewModel(get(), userId, username, category)
    }

    viewModel { (id: String, resources: Resources) -> TopicViewModel(get(), id, resources) }

    viewModel { (entryId: String) -> MediaInfoViewModel(get(), entryId) }
    viewModel { (entryId: String) -> EpisodeViewModel(get(), entryId) }
    viewModel { (entryId: String, sortCriteria: CommentSortCriteria) ->
        CommentsViewModel(get(), entryId, sortCriteria)
    }
    viewModel { (entryId: String) -> RelationViewModel(get(), entryId) }
    viewModel { (entryId: String) -> EntryCharactersViewModel(get(), entryId) }
    viewModel { (entryId: String) -> EntryPersonsViewModel(get(), entryId) }
    viewModel { (characterId: String) -> CharacterViewModel(get(), characterId) }
    viewModel { (personId: String) -> PersonViewModel(get(), personId) }
    viewModel { (entryId: String) -> RecommendationViewModel(get(), entryId) }
    viewModel { (entryId: String) -> DiscussionViewModel(get(), entryId) }

    viewModel { (industryId: String) -> IndustryInfoViewModel(get(), industryId) }
    viewModel { (industryId: String) -> IndustryProjectViewModel(get(), industryId) }
    viewModel { (translatorGroupId: String) -> TranslatorGroupInfoViewModel(get(), translatorGroupId) }
    viewModel { (translatorGroupId: String) -> TranslatorGroupProjectViewModel(get(), translatorGroupId) }

    viewModel { (entryId: String, language: Language, episode: Int) ->
        MangaViewModel(get(), entryId, language, episode)
    }
    viewModel { (entryId: String, language: AnimeLanguage, episode: Int) ->
        AnimeViewModel(get(), entryId, language, episode)
    }

    viewModel { (id: String?, entryId: String?) -> EditCommentViewModel(get(), id, entryId) }

    viewModel { LinkCheckViewModel(get()) }
}

val koinModules = listOf(storageModule, networkModule, appModule, viewModelModule)
