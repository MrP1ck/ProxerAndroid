package me.proxer.app.profile.settings

import android.app.Activity
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.settings.ListPreference
import me.proxer.app.settings.PreferenceCategory
import me.proxer.app.settings.choices
import me.proxer.app.ui.components.LoadingState
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.showErrorSnackbar
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.extension.startActivity
import me.proxer.library.enums.UcpSettingConstraint
import me.proxer.library.util.ProxerUtils
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

/**
 * The settings of the profile on Proxer: the visibility of its parts and the video ads.
 *
 * @author Ruben Gees
 */
class ProfileSettingsActivity : ComposeActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ProfileSettingsActivity>()

        private val constraints = listOf(
            ConstraintSetting(R.string.profile_preference_profile, { it.profileVisibility }, { copy(profileVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_topten, { it.topTenVisibility }, { copy(topTenVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_anime, { it.animeVisibility }, { copy(animeVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_manga, { it.mangaVisibility }, { copy(mangaVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_comment, { it.commentVisibility }, { copy(commentVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_forum, { it.forumVisibility }, { copy(forumVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_friend, { it.friendVisibility }, { copy(friendVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_friend_request,
                { it.friendRequestConstraint },
                { copy(friendRequestConstraint = it) }
            ),
            ConstraintSetting(R.string.profile_preference_about, { it.aboutVisibility }, { copy(aboutVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_history, { it.historyVisibility }, { copy(historyVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_guest_book, { it.guestBookVisibility }, { copy(guestBookVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_guest_book_entry,
                { it.guestBookEntryConstraint },
                { copy(guestBookEntryConstraint = it) }
            ),
            ConstraintSetting(R.string.profile_preference_gallery, { it.galleryVisibility }, { copy(galleryVisibility = it) }),
            ConstraintSetting(R.string.profile_preference_article, { it.articleVisibility }, { copy(articleVisibility = it) })
        )
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val viewModel = koinViewModel<ProfileSettingsViewModel>()
        val user by rememberCurrentUser()
        val settings by viewModel.data.observeAsState()

        ProxerScaffold(
            title = stringResource(R.string.section_profile_settings),
            onNavigateUp = navigator::navigateUp
        ) { padding ->
            Errors(viewModel)

            LaunchedEffect(user) { if (user == null) finish() }

            when (val safeSettings = settings) {
                null -> LoadingState()
                else -> LazyColumn(contentPadding = padding) {
                    item(key = "ads") { AdSettings(safeSettings, viewModel::update) }

                    item(key = "privacy") {
                        PreferenceCategory(stringResource(R.string.profile_preference_privacy_category_title))
                    }

                    items(constraints, key = { it.title }) { (title, getter, copier) ->
                        ConstraintPreference(
                            title = title,
                            isFriendRequest = title == R.string.profile_preference_friend_request,
                            selected = getter(safeSettings),
                            onSelect = { viewModel.update(safeSettings.copier(it)) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Errors(viewModel: ProfileSettingsViewModel) {
        val context = LocalContext.current
        val snackbarHostState = LocalSnackbarHostState.current
        val error by viewModel.error.observeAsState()
        val updateError by viewModel.updateError.observeAsState()
        val onRefreshErrorAction = rememberErrorActionHandler(viewModel::refresh)
        val onUpdateErrorAction = rememberErrorActionHandler(viewModel::retryUpdate)

        LaunchedEffect(error) {
            error?.let {
                snackbarHostState?.showErrorSnackbar(
                    context = context,
                    error = it,
                    onAction = onRefreshErrorAction,
                    message = context.getString(R.string.error_refresh, context.getString(it.message))
                )
            }
        }

        LaunchedEffect(updateError) {
            updateError?.let {
                snackbarHostState?.showErrorSnackbar(
                    context = context,
                    error = it,
                    onAction = onUpdateErrorAction,
                    message = context.getString(R.string.error_set_user_info, context.getString(it.message))
                )
            }
        }
    }

    @Composable
    private fun AdSettings(settings: LocalProfileSettings, onUpdate: (LocalProfileSettings) -> Unit) {
        val context = LocalContext.current
        val intervals = stringArrayResource(R.array.profile_settings_video_ads_interval_values).map { it.toInt() }
        val normalizedInterval = intervals.sortedDescending().find { settings.adInterval >= it } ?: 0

        // The banner ads setting is hidden until banner ads are actually implemented.
        PreferenceCategory(stringResource(R.string.profile_preference_ads_category_title))

        ListPreference(
            title = stringResource(R.string.profile_preference_video_ads_title),
            entries = choices(R.array.profile_settings_video_ads_interval_titles, R.array.profile_settings_video_ads_interval_values),
            selected = normalizedInterval.toString(),
            onSelect = { onUpdate(settings.copy(adInterval = it.toInt())) },
            summary = { context.getString(R.string.profile_preference_video_ads_summary, it.lowercase(Locale.GERMANY)) }
        )
    }

    @Composable
    private fun ConstraintPreference(
        @StringRes title: Int,
        isFriendRequest: Boolean,
        selected: UcpSettingConstraint,
        onSelect: (UcpSettingConstraint) -> Unit
    ) {
        val entries = when (isFriendRequest) {
            true -> choices(
                R.array.profile_settings_friend_request_constraint_titles,
                R.array.profile_settings_friend_request_constraint_values
            )
            false -> choices(R.array.profile_settings_constraint_titles, R.array.profile_settings_constraint_values)
        }

        ListPreference(
            title = stringResource(title),
            entries = entries,
            selected = ProxerUtils.getSafeApiEnumName(selected),
            onSelect = { onSelect(ProxerUtils.toSafeApiEnum<UcpSettingConstraint>(it)) }
        )
    }
}

private data class ConstraintSetting(
    @StringRes val title: Int,
    val get: (LocalProfileSettings) -> UcpSettingConstraint,
    val set: LocalProfileSettings.(UcpSettingConstraint) -> LocalProfileSettings
)
