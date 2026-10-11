package me.proxer.app.chat

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import me.proxer.app.R
import me.proxer.app.chat.prv.ConferenceWithMessage
import me.proxer.app.chat.prv.conference.ConferenceFragmentPingEvent
import me.proxer.app.chat.prv.conference.ConferenceViewModel
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.RegisterWhileResumed
import me.proxer.app.ui.components.SearchableTopAppBar
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.AccountButton
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

private const val SEARCH_DEBOUNCE_MILLIS = 200L

/**
 * The private conferences of the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerScreen(conferenceViewModel: ConferenceViewModel = koinViewModel(parameters = { parametersOf("") })) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val userId = koinInject<StorageHelper>().user?.id

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MILLIS)

        conferenceViewModel.searchQuery = query.trim()
    }

    RegisterWhileResumed(ConferenceFragmentPingEvent::class.java)

    LifecycleResumeEffect(Unit) {
        MessengerNotifications.cancel(context)

        onPauseOrDispose { }
    }

    val conferenceState = conferenceViewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(conferenceViewModel::load)
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ProxerScaffold(
        topBar = {
            SearchableTopAppBar(
                title = stringResource(R.string.section_messenger),
                isSearching = isSearching,
                query = query,
                onQueryChange = { query = it },
                onSearchingChange = { searching ->
                    isSearching = searching

                    if (!searching) query = ""
                },
                searchHint = stringResource(R.string.chat_search_hint),
                actions = { AccountButton() },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (conferenceState.data != null) {
                CreateConferenceButton(
                    onCreateChat = { navigator.openCreateConference(isGroup = false) },
                    onCreateGroup = { navigator.openCreateConference(isGroup = true) }
                )
            }
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        ConferenceList(
            state = conferenceState,
            userId = userId,
            isSearching = query.isNotBlank(),
            onErrorAction = onErrorAction,
            onConferenceClick = { navigator.openConference(it.conference) },
            contentPadding = padding
        )
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
fun ConferenceList(
    state: ContentState<List<ConferenceWithMessage>>,
    userId: String?,
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
                ConferenceItem(item, userId, onClick = { onConferenceClick(item) })
            }
        }
    }
}

@Composable
private fun ConferenceItem(item: ConferenceWithMessage, userId: String?, onClick: () -> Unit) {
    val context = LocalContext.current
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
                    color = when (isUnread) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.previewText(context, userId),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isUnread) FontWeight.Medium else null,
                    color = when (isUnread) {
                        true -> MaterialTheme.colorScheme.onSurface
                        false -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
                painter = painterResource(
                    if (conference.isGroup) R.drawable.ic_symbol_group else R.drawable.ic_symbol_person
                ),
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
