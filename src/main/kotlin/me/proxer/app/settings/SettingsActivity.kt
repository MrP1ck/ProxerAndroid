package me.proxer.app.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import kotlinx.coroutines.launch
import me.proxer.app.BuildConfig
import me.proxer.app.DEFAULT_PREFERENCES
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.prv.sync.MessengerWorker
import me.proxer.app.notification.NotificationWorker
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.ThemePicker
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.PreferenceHelper.Companion.AGE_CONFIRMATION
import me.proxer.app.util.data.PreferenceHelper.Companion.AUTO_BOOKMARK
import me.proxer.app.util.data.PreferenceHelper.Companion.CHECK_CELLULAR
import me.proxer.app.util.data.PreferenceHelper.Companion.EXTERNAL_CACHE
import me.proxer.app.util.data.PreferenceHelper.Companion.HTTP_LOG_LEVEL
import me.proxer.app.util.data.PreferenceHelper.Companion.HTTP_REDACT_TOKEN
import me.proxer.app.util.data.PreferenceHelper.Companion.HTTP_VERBOSE
import me.proxer.app.util.data.PreferenceHelper.Companion.LINK_CHECK
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_ACCOUNT
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_CHAT
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_INTERVAL
import me.proxer.app.util.data.PreferenceHelper.Companion.NOTIFICATIONS_NEWS
import me.proxer.app.util.data.PreferenceHelper.Companion.START_PAGE
import me.proxer.app.util.extension.clearTop
import me.proxer.app.util.extension.startActivity
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import kotlin.system.exitProcess

/**
 * The settings of the app. They are stored in the default shared preferences, with the same keys as before.
 */
class SettingsActivity : ComposeActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<SettingsActivity>()
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current

        ProxerScaffold(
            title = stringResource(R.string.section_settings),
            onNavigateUp = navigator::navigateUp
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                item { AccountSettings() }
                item { AppearanceSettings() }
                item { GeneralSettings() }
                item { NotificationSettings() }

                if (BuildConfig.DEBUG || BuildConfig.LOG) {
                    item { DeveloperSettings() }
                }
            }
        }
    }

    @Composable
    private fun AccountSettings() {
        val navigator = LocalAppNavigator.current
        val user by rememberCurrentUser()

        Preference(
            title = stringResource(R.string.section_profile_settings),
            summary = stringResource(R.string.preference_profile_summary),
            icon = R.drawable.ic_symbol_manage_accounts,
            enabled = user != null,
            onClick = navigator::openProfileSettings,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    @Composable
    private fun AppearanceSettings() {
        val preferenceHelper = koinInject<PreferenceHelper>()
        val themeContainer by preferenceHelper.themeFlow.collectAsState()

        PreferenceCategory(stringResource(R.string.preference_category_design_title))

        ThemePicker(
            themeContainer = themeContainer,
            onThemeContainerChange = { preferenceHelper.themeContainer = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        SwitchPreference(
            title = stringResource(R.string.theme_amoled),
            summary = stringResource(R.string.theme_amoled_summary),
            checked = themeContainer.isAmoled,
            onCheckedChange = { preferenceHelper.themeContainer = themeContainer.copy(isAmoled = it) }
        )
    }

    @Composable
    private fun GeneralSettings() {
        val preferences = defaultPreferences()
        val isAgeConfirmed by preferences.observeAsState(AGE_CONFIRMATION) { getBoolean(AGE_CONFIRMATION, false) }
        val autoBookmark by preferences.observeAsState(AUTO_BOOKMARK) { getBoolean(AUTO_BOOKMARK, false) }
        val checkCellular by preferences.observeAsState(CHECK_CELLULAR) { getBoolean(CHECK_CELLULAR, true) }
        val checkLinks by preferences.observeAsState(LINK_CHECK) { getBoolean(LINK_CHECK, true) }
        val externalCache by preferences.observeAsState(EXTERNAL_CACHE) { getBoolean(EXTERNAL_CACHE, true) }
        val startPage by preferences.observeAsState(START_PAGE) { getString(START_PAGE, "0") ?: "0" }
        val showRestartMessage = rememberRestartMessage()

        PreferenceCategory(stringResource(R.string.preference_category_general_title))

        SwitchPreference(
            title = stringResource(R.string.preference_age_confirmation_title),
            summary = summaryOf(
                isAgeConfirmed,
                R.string.preference_age_confirmation_summary_on,
                R.string.preference_age_confirmation_summary_off
            ),
            checked = isAgeConfirmed,
            onCheckedChange = {
                when (it) {
                    true -> AgeConfirmationDialog.show(this@SettingsActivity)
                    false -> preferences.edit { putBoolean(AGE_CONFIRMATION, false) }
                }
            }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_auto_bookmark_title),
            summary = summaryOf(
                autoBookmark,
                R.string.preference_auto_bookmark_summary_on,
                R.string.preference_auto_bookmark_summary_off
            ),
            checked = autoBookmark,
            onCheckedChange = { preferences.edit { putBoolean(AUTO_BOOKMARK, it) } }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_check_wifi_title),
            summary = summaryOf(
                checkCellular,
                R.string.preference_check_wifi_summary_on,
                R.string.preference_check_wifi_summary_off
            ),
            checked = checkCellular,
            onCheckedChange = { preferences.edit { putBoolean(CHECK_CELLULAR, it) } }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_check_links_title),
            summary = summaryOf(
                checkLinks,
                R.string.preference_check_links_summary_on,
                R.string.preference_check_links_summary_off
            ),
            checked = checkLinks,
            onCheckedChange = { preferences.edit { putBoolean(LINK_CHECK, it) } }
        )

        val hasRemovableStorage = !Environment.isExternalStorageEmulated() &&
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

        if (hasRemovableStorage) {
            SwitchPreference(
                title = stringResource(R.string.preference_external_cache_title),
                summary = summaryOf(
                    externalCache,
                    R.string.preference_external_cache_summary_on,
                    R.string.preference_external_cache_summary_off
                ),
                checked = externalCache,
                onCheckedChange = {
                    preferences.edit { putBoolean(EXTERNAL_CACHE, it) }

                    showRestartMessage()
                }
            )
        }

        ListPreference(
            title = stringResource(R.string.preference_start_page_title),
            entries = choices(R.array.start_page_titles, R.array.start_page_values),
            selected = startPage,
            onSelect = { preferences.edit { putString(START_PAGE, it) } }
        )
    }

    @Composable
    private fun NotificationSettings() {
        val context = LocalContext.current
        val preferences = defaultPreferences()
        val news by preferences.observeAsState(NOTIFICATIONS_NEWS) { getBoolean(NOTIFICATIONS_NEWS, false) }
        val account by preferences.observeAsState(NOTIFICATIONS_ACCOUNT) { getBoolean(NOTIFICATIONS_ACCOUNT, false) }
        val chat by preferences.observeAsState(NOTIFICATIONS_CHAT) { getBoolean(NOTIFICATIONS_CHAT, true) }
        val interval by preferences.observeAsState(NOTIFICATIONS_INTERVAL) {
            getString(NOTIFICATIONS_INTERVAL, "30") ?: "30"
        }

        val snackbarHostState = LocalSnackbarHostState.current
        val scope = rememberCoroutineScope()

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                scope.launch {
                    val result = snackbarHostState?.showSnackbar(
                        message = context.getString(R.string.preference_notifications_permission_denied),
                        actionLabel = context.getString(R.string.preference_notifications_permission_action),
                        duration = SnackbarDuration.Long
                    )

                    if (result == SnackbarResult.ActionPerformed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        )
                    }
                }
            }
        }

        fun update(key: String, enabled: Boolean, onChanged: () -> Unit) {
            preferences.edit { putBoolean(key, enabled) }
            onChanged()

            if (
                enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !NotificationManagerCompat.from(context).areNotificationsEnabled()
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        PreferenceCategory(stringResource(R.string.preference_category_notifications_title))

        SwitchPreference(
            title = stringResource(R.string.preference_notifications_news_title),
            summary = summaryOf(
                news,
                R.string.preference_notifications_summary_on,
                R.string.preference_notifications_summary_off
            ),
            checked = news,
            onCheckedChange = { update(NOTIFICATIONS_NEWS, it) { NotificationWorker.enqueueIfPossible() } }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_notifications_account_title),
            summary = summaryOf(
                account,
                R.string.preference_notifications_summary_on,
                R.string.preference_notifications_summary_off
            ),
            checked = account,
            onCheckedChange = { update(NOTIFICATIONS_ACCOUNT, it) { NotificationWorker.enqueueIfPossible() } }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_notifications_chat_title),
            summary = summaryOf(
                chat,
                R.string.preference_notifications_summary_on,
                R.string.preference_notifications_summary_off
            ),
            checked = chat,
            onCheckedChange = {
                update(NOTIFICATIONS_CHAT, it) { MessengerWorker.enqueueSynchronizationIfPossible() }
            }
        )

        ListPreference(
            title = stringResource(R.string.preference_notifications_interval_title),
            entries = choices(R.array.notifications_interval_titles, R.array.notifications_interval_values),
            selected = interval,
            enabled = news || account,
            onSelect = {
                preferences.edit { putString(NOTIFICATIONS_INTERVAL, it) }

                NotificationWorker.enqueueIfPossible()
                MessengerWorker.enqueueSynchronizationIfPossible()
            }
        )
    }

    @Composable
    private fun DeveloperSettings() {
        val preferences = defaultPreferences()
        val logLevel by preferences.observeAsState(HTTP_LOG_LEVEL) { getString(HTTP_LOG_LEVEL, "0") ?: "0" }
        val verbose by preferences.observeAsState(HTTP_VERBOSE) { getBoolean(HTTP_VERBOSE, false) }
        val redactToken by preferences.observeAsState(HTTP_REDACT_TOKEN) { getBoolean(HTTP_REDACT_TOKEN, true) }
        val showRestartMessage = rememberRestartMessage()

        PreferenceCategory(stringResource(R.string.preference_category_developer_option))

        ListPreference(
            title = stringResource(R.string.preference_developer_options_http_log_level_title),
            entries = choices(R.array.http_log_level_titles, R.array.http_log_level_values),
            selected = logLevel,
            onSelect = {
                preferences.edit { putString(HTTP_LOG_LEVEL, it) }

                showRestartMessage()
            }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_developer_options_http_log_verbose_title),
            summary = summaryOf(
                verbose,
                R.string.preference_developer_options_http_log_verbose_summary_on,
                R.string.preference_developer_options_http_log_verbose_summary_off
            ),
            checked = verbose,
            onCheckedChange = {
                preferences.edit { putBoolean(HTTP_VERBOSE, it) }

                showRestartMessage()
            }
        )

        SwitchPreference(
            title = stringResource(R.string.preference_developer_options_http_redact_token_title),
            summary = summaryOf(
                redactToken,
                R.string.preference_developer_options_http_redact_token_summary_on,
                R.string.preference_developer_options_http_redact_token_summary_off
            ),
            checked = redactToken,
            onCheckedChange = {
                preferences.edit { putBoolean(HTTP_REDACT_TOKEN, it) }

                showRestartMessage()
            }
        )
    }

    @Composable
    private fun defaultPreferences(): SharedPreferences = koinInject(named(DEFAULT_PREFERENCES))

    /**
     * Returns a function showing a snackbar which tells the user that a setting takes effect after a restart.
     */
    @Composable
    private fun rememberRestartMessage(): () -> Unit {
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current
        val scope = rememberCoroutineScope()

        return {
            scope.launch {
                val result = snackbarHostState?.showSnackbar(
                    message = context.getString(R.string.fragment_settings_restart_message),
                    actionLabel = context.getString(R.string.fragment_settings_restart_action),
                    duration = SnackbarDuration.Long
                )

                if (result == SnackbarResult.ActionPerformed) {
                    packageManager.getLaunchIntentForPackage(packageName)?.clearTop()?.let { startActivity(it) }

                    exitProcess(0)
                }
            }
        }
    }
}
