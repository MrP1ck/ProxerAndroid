package me.proxer.app.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.conference.ConferenceViewModel
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.chat.pub.room.ChatRoomViewModel
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SearchableTopAppBar
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.rememberLinkifiedText
import me.proxer.app.ui.preview.PreviewData
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import me.proxer.app.ui.shell.AccountButton
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.library.entity.chat.ChatRoom
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private const val PUBLIC_PAGE = 0
private const val PRIVATE_PAGE = 1
private const val SEARCH_DEBOUNCE_MILLIS = 200L

/**
 * The public chat rooms and the private conferences of the user in two tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    showMessenger: Boolean,
    chatRoomViewModel: ChatRoomViewModel = koinViewModel(),
    conferenceViewModel: ConferenceViewModel = koinViewModel(parameters = { parametersOf("") })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = if (showMessenger) PRIVATE_PAGE else PUBLIC_PAGE) { 2 }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MILLIS)

        conferenceViewModel.searchQuery = query.trim()
    }

    LaunchedEffect(showMessenger) {
        if (showMessenger) pagerState.animateScrollToPage(PRIVATE_PAGE)
    }

    LifecycleResumeEffect(Unit) {
        MessengerNotifications.cancel(context)

        onPauseOrDispose { }
    }

    val roomState = chatRoomViewModel.collectContentState()
    val conferenceState = conferenceViewModel.collectContentState()
    val onRoomErrorAction = rememberErrorActionHandler(chatRoomViewModel::load)
    val onConferenceErrorAction = rememberErrorActionHandler(conferenceViewModel::load)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ProxerScaffold(
        topBar = {
            Column {
                SearchableTopAppBar(
                    title = stringResource(R.string.section_chat),
                    isSearching = isSearching,
                    query = query,
                    onQueryChange = { query = it },
                    onSearchingChange = { searching ->
                        isSearching = searching

                        if (searching) {
                            scope.launch { pagerState.animateScrollToPage(PRIVATE_PAGE) }
                        } else {
                            query = ""
                        }
                    },
                    searchHint = stringResource(R.string.chat_search_hint),
                    actions = { AccountButton() },
                    scrollBehavior = scrollBehavior
                )

                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    Tab(
                        selected = pagerState.currentPage == PUBLIC_PAGE,
                        onClick = { scope.launch { pagerState.animateScrollToPage(PUBLIC_PAGE) } },
                        text = { Text(stringResource(R.string.fragment_chat_container_public)) }
                    )

                    Tab(
                        selected = pagerState.currentPage == PRIVATE_PAGE,
                        onClick = { scope.launch { pagerState.animateScrollToPage(PRIVATE_PAGE) } },
                        text = { Text(stringResource(R.string.fragment_chat_container_private)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (pagerState.currentPage == PRIVATE_PAGE && conferenceState.data != null) {
                CreateConferenceButton(
                    onCreateChat = { navigator.openCreateConference(isGroup = false) },
                    onCreateGroup = { navigator.openCreateConference(isGroup = true) }
                )
            }
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                PUBLIC_PAGE -> ChatRoomList(
                    state = roomState,
                    onErrorAction = onRoomErrorAction,
                    onRoomClick = { navigator.openChatRoom(it.id, it.name, it.isReadOnly) },
                    onLinkClick = navigator::showPage,
                    contentPadding = padding
                )
                else -> ConferenceList(
                    state = conferenceState,
                    isSearching = query.isNotBlank(),
                    onErrorAction = onConferenceErrorAction,
                    onConferenceClick = { navigator.openConference(it.conference) },
                    contentPadding = padding
                )
            }
        }
    }
}

@Composable
private fun CreateConferenceButton(onCreateChat: () -> Unit, onCreateGroup: () -> Unit) {
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }

    Box {
        ExtendedFloatingActionButton(
            onClick = { isMenuVisible = true },
            icon = { Icon(painterResource(R.drawable.ic_symbol_edit), contentDescription = null) },
            text = { Text(stringResource(R.string.action_create_conference)) }
        )

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
                text = { Text(stringResource(R.string.action_create_group)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_group_add), contentDescription = null) },
                onClick = {
                    isMenuVisible = false
                    onCreateGroup()
                }
            )
        }
    }
}

@Composable
fun ChatRoomList(
    state: ContentState<List<ChatRoom>>,
    onErrorAction: (ErrorAction) -> Unit,
    onRoomClick: (ChatRoom) -> Unit,
    onLinkClick: (HttpUrl) -> Unit,
    contentPadding: PaddingValues
) {
    ContentStateHost(state = state, onErrorAction = onErrorAction, contentPadding = contentPadding) { rooms ->
        LazyColumn(contentPadding = contentPadding.plus(PaddingValues(vertical = 8.dp))) {
            items(rooms, key = { it.id }) { room ->
                ListItem(
                    headlineContent = { Text(room.name, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = if (room.topic.isNotBlank()) {
                        { Text(rememberLinkifiedText(room.topic.trim(), onLinkClick)) }
                    } else {
                        null
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (room.isReadOnly) R.drawable.ic_symbol_visibility else R.drawable.ic_symbol_forum
                                ),
                                contentDescription = if (room.isReadOnly) stringResource(R.string.chat_read_only) else null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    },
                    modifier = Modifier.clickable { onRoomClick(room) }
                )
            }
        }
    }
}

@Composable
fun ConferenceList(
    state: ContentState<List<ConferenceWithMessage>>,
    isSearching: Boolean,
    onErrorAction: (ErrorAction) -> Unit,
    onConferenceClick: (ConferenceWithMessage) -> Unit,
    contentPadding: PaddingValues
) {
    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = if (isSearching) R.string.error_no_data_search else R.string.error_no_data_conferences
    ) { conferences ->
        // Space for the floating action button.
        LazyColumn(contentPadding = contentPadding.plus(PaddingValues(top = 8.dp, bottom = 88.dp))) {
            items(conferences, key = { it.conference.id }) { item ->
                ConferenceItem(item, onClick = { onConferenceClick(item) })
            }
        }
    }
}

@Composable
private fun ConferenceItem(item: ConferenceWithMessage, onClick: () -> Unit) {
    val context = LocalContext.current
    val storageHelper = koinInject<StorageHelper>()
    val conference = item.conference
    val isUnread = !conference.localIsRead

    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conference.topic,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = conference.date.toLocalDateTime().distanceInWordsToNow(context),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.previewText(context, storageHelper.user?.id),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isUnread) FontWeight.Medium else null,
                    color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                if (isUnread) {
                    val description = pluralStringResource(
                        R.plurals.chat_unread_messages,
                        conference.unreadMessageAmount,
                        conference.unreadMessageAmount
                    )

                    Badge(Modifier.semanticsDescription(description)) {
                        Text(conference.unreadMessageAmount.toString())
                    }
                }
            }
        },
        leadingContent = { ConferenceAvatar(item) },
        modifier = Modifier.clickable(onClick = onClick)
    )

    HorizontalDivider(Modifier.padding(start = 88.dp))
}

@Composable
private fun ConferenceAvatar(item: ConferenceWithMessage) {
    val conference = item.conference

    if (conference.image.isBlank()) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(if (conference.isGroup) R.drawable.ic_symbol_group else R.drawable.ic_symbol_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    } else {
        ProxerAsyncImage(
            url = ProxerUrls.userImage(conference.image),
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            showErrorIcon = false
        )
    }
}

private fun ConferenceWithMessage.previewText(context: Context, userId: String?): String {
    val message = message ?: return ""
    val isFromUser = message.userId == userId

    val text = message.messageText
        .replace("\r\n", " ")
        .replace("\n", " ")
        .trim()

    val prefixedText = when {
        conference.isGroup && !isFromUser -> "${message.username}: $text"
        else -> text
    }

    val statusPrefix = when {
        !isFromUser -> ""
        message.messageId < 0 -> "🕓 "
        else -> "✓ "
    }

    return statusPrefix + message.messageAction.toAppString(context, message.username, prefixedText)
}

private fun Modifier.semanticsDescription(description: String) = this.then(
    Modifier.semantics { contentDescription = description }
)

@ThemePreviews
@Composable
private fun ChatRoomListPreview() = PreviewSurface {
    ChatRoomList(
        state = ContentState(PreviewData.chatRooms),
        onErrorAction = {},
        onRoomClick = {},
        onLinkClick = {},
        contentPadding = PaddingValues()
    )
}
