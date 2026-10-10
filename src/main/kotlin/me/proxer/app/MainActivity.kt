package me.proxer.app

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseActivity
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.profile.settings.ProfileSettingsViewModel
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.applyThemeContainer
import me.proxer.app.ui.shell.Onboarding
import me.proxer.app.ui.shell.ProxerApp
import me.proxer.app.ui.shell.SectionRequest
import me.proxer.app.ui.theme.ProxerAppTheme
import me.proxer.app.ui.view.RatingDialog
import me.proxer.app.util.InAppUpdateFlow
import me.proxer.app.util.extension.intentFor
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

/**
 * The single Activity hosting the Compose UI of the app. Sections can be opened through [getSectionIntent] and deep
 * links (see the intent filters in the manifest).
 *
 * @author Ruben Gees
 */
class MainActivity : BaseActivity() {

    companion object {
        private const val SECTION_EXTRA = "section"
        private const val SECTION_ACTION_PREFIX = "me.proxer.app.intent.action."

        fun navigateToSection(context: Context, section: MainSection) = context
            .startActivity(getSectionIntent(context, section))

        fun getSectionIntent(context: Context, section: MainSection): Intent = context
            .intentFor<MainActivity>(SECTION_EXTRA to section)
            .setAction(SECTION_ACTION_PREFIX + section.name)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    private val profileSettingsViewModel by viewModel<ProfileSettingsViewModel>()

    private val inAppUpdateFlow = InAppUpdateFlow()
    private val snackbarHostState = SnackbarHostState()
    private val sectionRequests = Channel<SectionRequest>(Channel.UNLIMITED)

    private val notificationPermissionRequest = registerForActivityResult(RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // The navigation bar of the app extends behind the system navigation, so no scrim is needed.
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))

        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        // Request storage permission for writing logs in debug variants.
        if (BuildConfig.LOG && VERSION.SDK_INT < VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        val isFirstLaunch = savedInstanceState == null && intent.action == Intent.ACTION_MAIN &&
            preferenceHelper.launches <= 0

        if (savedInstanceState == null) {
            intent.toSectionRequest()?.let { sectionRequests.trySend(it) }

            onFreshStart(isFirstLaunch)
        }

        setContent {
            ProxerAppTheme {
                var isOnboardingVisible by rememberSaveable { mutableStateOf(isFirstLaunch) }

                ProxerApp(
                    activity = this,
                    startSection = preferenceHelper.startPage,
                    sectionRequests = sectionRequests.receiveAsFlow(),
                    snackbarHostState = snackbarHostState
                )

                if (isOnboardingVisible) {
                    val themeContainer by preferenceHelper.themeFlow.collectAsStateWithLifecycle()

                    Onboarding(
                        themeContainer = themeContainer,
                        onThemeContainerChange = { preferenceHelper.themeContainer = it },
                        onFinish = { areNotificationsEnabled ->
                            isOnboardingVisible = false

                            onOnboardingFinished(areNotificationsEnabled)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.action != Intent.ACTION_MAIN) {
            this.intent = intent
        }

        intent.toSectionRequest()?.let { sectionRequests.trySend(it) }
    }

    override fun onDestroy() {
        inAppUpdateFlow.stop()

        super.onDestroy()
    }

    /**
     * The Compose UI follows the theme without recreating the Activity. The View theme is updated for the dialogs,
     * which are still View based.
     */
    override fun onThemeChanged(themeContainer: ThemeContainer) {
        applyThemeContainer(themeContainer)
    }

    private fun onFreshStart(isFirstLaunch: Boolean) {
        if (intent.action == Intent.ACTION_MAIN) {
            preferenceHelper.incrementLaunches()

            preferenceHelper.launches.let { launches ->
                if (launches >= 3 && launches % 3 == 0 && !preferenceHelper.hasRated) {
                    RatingDialog.show(this)
                }
            }
        }

        if (storageHelper.isLoggedIn) {
            val lastUcpSettingsUpdate = storageHelper.lastUcpSettingsUpdateDate
            val threshold = Instant.now().minus(5, ChronoUnit.MINUTES)

            if (threshold.isAfter(lastUcpSettingsUpdate)) {
                profileSettingsViewModel.refresh()
            }
        }

        if (!isFirstLaunch) {
            requestNotificationPermissionIfNeeded()
            startInAppUpdateFlow()
        }
    }

    private fun onOnboardingFinished(areNotificationsEnabled: Boolean) {
        preferenceHelper.areNewsNotificationsEnabled = areNotificationsEnabled
        preferenceHelper.areAccountNotificationsEnabled = areNotificationsEnabled

        NotificationWorker.enqueueIfPossible(delay = true)

        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        val areNotificationsEnabled = preferenceHelper.areNewsNotificationsEnabled ||
            preferenceHelper.areAccountNotificationsEnabled ||
            preferenceHelper.areChatNotificationsEnabled

        if (
            VERSION.SDK_INT >= VERSION_CODES.TIRAMISU && areNotificationsEnabled &&
            ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED
        ) {
            notificationPermissionRequest.launch(POST_NOTIFICATIONS)
        }
    }

    private fun startInAppUpdateFlow() = inAppUpdateFlow.start(
        this,
        object : InAppUpdateFlow.Callbacks {
            override fun onUpdateAvailable(startDownload: () -> Unit) = showUpdateSnackbar(
                R.string.activity_update_available,
                R.string.activity_update_action_download,
                startDownload
            )

            override fun onUpdateDownloaded(install: () -> Unit) = showUpdateSnackbar(
                R.string.activity_update_ready,
                R.string.activity_update_action_install,
                install
            )

            override fun onUpdateCancelled() {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    )

    private fun showUpdateSnackbar(message: Int, action: Int, onAction: () -> Unit) {
        lifecycleScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = getString(message),
                actionLabel = getString(action),
                duration = SnackbarDuration.Indefinite
            )

            if (result == SnackbarResult.ActionPerformed) onAction()
        }
    }

    private fun Intent.toSectionRequest(): SectionRequest? {
        if (action == Intent.ACTION_VIEW) {
            val segments = data?.pathSegments ?: emptyList()

            val section = when (segments.firstOrNull()) {
                "news" -> MainSection.NEWS
                "chat" -> MainSection.CHAT
                "messages" -> MainSection.MESSENGER
                "reminder" -> MainSection.BOOKMARKS
                "anime" -> MainSection.ANIME
                "calendar" -> MainSection.SCHEDULE
                "manga" -> MainSection.MANGA
                else -> null
            }

            if (section != null) {
                return SectionRequest(
                    section = section,
                    type = deepLinkType(section, segments.getOrNull(1)),
                    sort = deepLinkSort(segments.getOrNull(2))
                )
            }
        }

        return (getSerializableExtra(SECTION_EXTRA) as? MainSection)?.let { SectionRequest(it) }
    }

    private fun deepLinkType(section: MainSection, segment: String?) = when (section) {
        MainSection.ANIME -> when (segment) {
            "animeseries" -> "ANIMESERIES"
            "movie" -> "MOVIE"
            "ova" -> "OVA"
            "hentai" -> "HENTAI"
            else -> null
        }
        MainSection.MANGA -> when (segment) {
            "mangaseries" -> "MANGASERIES"
            "oneshot" -> "ONESHOT"
            "doujin" -> "DOUJIN"
            "hmanga" -> "HMANGA"
            else -> null
        }
        else -> null
    }

    private fun deepLinkSort(segment: String?) = when (segment) {
        "rating" -> "RATING"
        "clicks" -> "CLICKS"
        else -> null
    }
}
