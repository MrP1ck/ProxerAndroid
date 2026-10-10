package me.proxer.app.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.R
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.profile.about.ProfileAboutTab
import me.proxer.app.profile.comment.ProfileCommentsTab
import me.proxer.app.profile.history.HistoryTab
import me.proxer.app.profile.info.ProfileInfoTab
import me.proxer.app.profile.media.ProfileMediaListTab
import me.proxer.app.profile.topten.TopTenTab
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.library.enums.Category
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

enum class ProfileTab(@StringRes val title: Int) {
    INFO(R.string.section_profile_info),
    ABOUT(R.string.section_profile_about),
    TOP_TEN(R.string.section_top_ten),
    ANIME(R.string.section_user_media_list_anime),
    MANGA(R.string.section_user_media_list_manga),
    COMMENTS(R.string.section_user_comments),
    HISTORY(R.string.section_user_history)
}

/**
 * The profile of a user: the points and status, the about page, the favorites, the anime and manga lists, the
 * comments and the history in tabs.
 */
@Composable
fun ProfileScreen(
    userId: String?,
    username: String?,
    initialImage: String?,
    initialTab: ProfileTab,
    viewModel: ProfileViewModel = koinViewModel(parameters = { parametersOf(userId, username) })
) {
    val navigator = LocalAppNavigator.current
    val messengerDao = koinInject<MessengerDao>()
    val scope = rememberCoroutineScope()
    val currentUser by rememberCurrentUser()
    val state = viewModel.collectContentState()
    val info = state.data?.info

    val name = info?.username ?: username
    val image = info?.image ?: initialImage
    val isOwnProfile = currentUser?.matches(userId ?: info?.id, name) == true

    val tabs = ProfileTab.values()
    val pagerState = rememberPagerState(initialPage = initialTab.ordinal) { tabs.size }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ProxerScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { UpButton(navigator::navigateUp) },
                    actions = {
                        if (!isOwnProfile && currentUser != null && name != null) {
                            ChatMenu(
                                onCreateChat = {
                                    scope.launch {
                                        val existing = withContext(Dispatchers.IO) {
                                            messengerDao.findConferenceForUser(name)
                                        }

                                        when (existing) {
                                            null -> navigator.openCreateConference(
                                                false,
                                                Participant(name, image ?: "")
                                            )
                                            else -> navigator.openConference(existing)
                                        }
                                    }
                                },
                                onCreateGroup = { navigator.openCreateConference(true, Participant(name, image ?: "")) }
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )

                PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(stringResource(tab.title)) }
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        // The lists need the id or name of the user, which deep links by name only provide after loading.
        val resolvedUserId = userId ?: info?.id
        val resolvedUsername = name

        HorizontalPager(state = pagerState, beyondViewportPageCount = 1, modifier = Modifier.fillMaxSize()) { page ->
            when (tabs[page]) {
                ProfileTab.INFO -> ProfileInfoTab(viewModel, state, image, padding)
                ProfileTab.ABOUT -> ProfileAboutTab(resolvedUserId, resolvedUsername, padding)
                ProfileTab.TOP_TEN -> TopTenTab(resolvedUserId, resolvedUsername, isOwnProfile, padding)
                ProfileTab.ANIME -> ProfileMediaListTab(
                    resolvedUserId,
                    resolvedUsername,
                    Category.ANIME,
                    isOwnProfile,
                    padding
                )
                ProfileTab.MANGA -> ProfileMediaListTab(
                    resolvedUserId,
                    resolvedUsername,
                    Category.MANGA,
                    isOwnProfile,
                    padding
                )
                ProfileTab.COMMENTS -> ProfileCommentsTab(resolvedUserId, resolvedUsername, padding)
                ProfileTab.HISTORY -> HistoryTab(resolvedUserId, resolvedUsername, padding)
            }
        }
    }
}

@Composable
private fun ChatMenu(onCreateChat: () -> Unit, onCreateGroup: () -> Unit) {
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isMenuVisible = true }) {
            Icon(
                painterResource(R.drawable.ic_symbol_chat),
                contentDescription = stringResource(R.string.action_create_chat)
            )
        }

        DropdownMenu(expanded = isMenuVisible, onDismissRequest = { isMenuVisible = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_create_chat)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_person), contentDescription = null) },
                onClick = {
                    isMenuVisible = false
                    onCreateChat()
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_new_group_with_user)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_group_add), contentDescription = null) },
                onClick = {
                    isMenuVisible = false
                    onCreateGroup()
                }
            )
        }
    }
}
