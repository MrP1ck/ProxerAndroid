package me.proxer.app.chat

import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.ui.components.BBCodeText
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.UserAvatar
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.TopAppBarTitle
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toast
import okhttp3.HttpUrl
import java.util.Date

private const val LOAD_MORE_THRESHOLD = 10

/**
 * A message of a conversation. Messages with an [actionText] (e.g. "X joined") are shown in the center.
 */
data class ConversationMessage(
    val id: String,
    val userId: String,
    val username: String,
    val imageUrl: HttpUrl?,
    val text: String,
    val content: BBTree?,
    val actionText: String?,
    val date: Date,
    val isPending: Boolean,
    val isSelf: Boolean
)

/**
 * The state of the input of a conversation. If not [isEnabled], [hint] explains why.
 */
data class ConversationInput(val isEnabled: Boolean, val hint: String)

/**
 * A conversation in the public chat or the messenger. The messages are ordered from the newest to the oldest.
 * Messages can be selected by long pressing them to copy, reply to or report them.
 */
@Composable
fun ConversationScreen(
    title: String,
    subtitle: String?,
    state: ContentState<List<ConversationMessage>>,
    input: ConversationInput,
    initialDraft: String?,
    showAuthors: Boolean,
    onNavigateUp: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    onLoadMore: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onAuthorClick: (ConversationMessage) -> Unit,
    onReport: ((ConversationMessage) -> Unit)?,
    modifier: Modifier = Modifier,
    @StringRes emptyMessage: Int = R.string.error_no_data_chat,
    onTitleClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val messages = state.data ?: emptyList()

    var selectedIds by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var draft by remember { mutableStateOf(TextFieldValue(initialDraft ?: "")) }

    LaunchedEffect(initialDraft) {
        if (initialDraft != null && draft.text.isBlank()) draft = TextFieldValue(initialDraft, TextRange(initialDraft.length))
    }

    val selectedMessages = messages.filter { it.id in selectedIds }.sortedBy { it.date }
    val isSelecting = selectedIds.isNotEmpty()

    BackHandler(enabled = isSelecting) { selectedIds = emptySet() }

    val isAtEnd by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(isAtEnd, messages.size) {
        if (isAtEnd && messages.isNotEmpty()) onLoadMore()
    }

    ProxerScaffold(
        topBar = {
            if (isSelecting) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onClose = { selectedIds = emptySet() },
                    onCopy = {
                        val text = selectedMessages.joinToString(separator = "\n") { it.text }

                        context.getSystemService<ClipboardManager>()?.setPrimaryClip(
                            ClipData.newPlainText(context.getString(R.string.fragment_messenger_clip_title), text)
                        )
                        context.toast(R.string.clipboard_status)

                        selectedIds = emptySet()
                    },
                    onReply = selectedMessages.singleOrNull()?.takeIf { !it.isSelf && input.isEnabled }?.let { message ->
                        {
                            val reply = context.getString(R.string.fragment_messenger_reply, message.username)

                            draft = TextFieldValue(reply, TextRange(reply.length))
                            selectedIds = emptySet()
                        }
                    },
                    onReport = onReport?.let { report ->
                        selectedMessages.singleOrNull()?.takeIf { !it.isSelf }?.let { message ->
                            {
                                report(message)
                                selectedIds = emptySet()
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Box(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier) {
                            TopAppBarTitle(title, subtitle)
                        }
                    },
                    navigationIcon = { UpButton(onNavigateUp) },
                    actions = actions
                )
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .imePadding()
        ) {
            Box(Modifier.weight(1f)) {
                ContentStateHost(
                    state = state,
                    onErrorAction = onErrorAction,
                    emptyMessage = emptyMessage
                ) {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                            // The list is reversed: the previous message is above, the next one below.
                            val previous = messages.getOrNull(index + 1)
                            val next = messages.getOrNull(index - 1)

                            MessageItem(
                                message = message,
                                isFirstOfGroup = previous == null || !previous.belongsToSameGroup(message),
                                isLastOfGroup = next == null || !next.belongsToSameGroup(message),
                                showAuthor = showAuthors,
                                isSelected = message.id in selectedIds,
                                onClick = {
                                    if (isSelecting) {
                                        selectedIds = if (message.id in selectedIds) selectedIds - message.id else selectedIds + message.id
                                    }
                                },
                                onLongClick = { if (message.actionText == null) selectedIds = selectedIds + message.id },
                                onAuthorClick = { onAuthorClick(message) }
                            )
                        }
                    }
                }

                val isScrolledUp by remember { derivedStateOf { listState.firstVisibleItemIndex > 2 } }

                if (isScrolledUp) {
                    SmallFloatingActionButton(
                        onClick = { scope.launch { listState.animateScrollToItem(0) } },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.ic_symbol_expand_more), contentDescription = null)
                    }
                }
            }

            InputBar(
                value = draft,
                onValueChange = {
                    draft = it
                    onDraftChange(it.text)
                },
                input = input,
                onSend = {
                    val text = draft.text.trim()

                    if (text.isNotBlank()) {
                        onSend(text)

                        draft = TextFieldValue("")
                        onDraftChange("")

                        scope.launch { listState.scrollToItem(0) }
                    }
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

private fun ConversationMessage.belongsToSameGroup(other: ConversationMessage) =
    userId == other.userId && actionText == null && other.actionText == null

@Composable
private fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onReply: (() -> Unit)?,
    onReport: (() -> Unit)?
) {
    TopAppBar(
        title = { Text(count.toString()) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(painterResource(R.drawable.ic_symbol_close), contentDescription = stringResource(R.string.cancel))
            }
        },
        actions = {
            if (onReply != null) {
                IconButton(onClick = onReply) {
                    Icon(painterResource(R.drawable.ic_symbol_reply), contentDescription = stringResource(R.string.action_reply))
                }
            }

            IconButton(onClick = onCopy) {
                Icon(painterResource(R.drawable.ic_symbol_content_copy), contentDescription = stringResource(R.string.action_copy))
            }

            if (onReport != null) {
                IconButton(onClick = onReport) {
                    Icon(painterResource(R.drawable.ic_symbol_flag), contentDescription = stringResource(R.string.action_report))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

@Composable
private fun MessageItem(
    message: ConversationMessage,
    isFirstOfGroup: Boolean,
    isLastOfGroup: Boolean,
    showAuthor: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAuthorClick: () -> Unit
) {
    val context = LocalContext.current

    if (message.actionText != null) {
        Text(
            text = message.actionText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        return
    }

    val showAvatar = showAuthor && !message.isSelf

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isFirstOfGroup) 8.dp else 2.dp),
        horizontalArrangement = if (message.isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (showAvatar) {
            Box(Modifier.width(40.dp)) {
                if (isFirstOfGroup) {
                    UserAvatar(message.imageUrl, Modifier.clickable(onClick = onAuthorClick))
                }
            }

            Spacer(Modifier.width(8.dp))
        }

        val bubbleColor = when {
            isSelected -> MaterialTheme.colorScheme.tertiaryContainer
            message.isSelf -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

        val shape = bubbleShape(message.isSelf, isFirstOfGroup, isLastOfGroup)

        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(shape)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (showAvatar && isFirstOfGroup) {
                    Text(
                        text = message.username,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onAuthorClick)
                    )
                }

                if (message.content != null) {
                    BBCodeText(message.content, userId = message.userId)
                } else {
                    Text(message.text, style = MaterialTheme.typography.bodyMedium)
                }

                if (isLastOfGroup || message.isPending) {
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.date.distanceInWordsToNow(context),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (message.isPending) {
                            Icon(
                                painterResource(R.drawable.ic_symbol_schedule),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun bubbleShape(isSelf: Boolean, isFirst: Boolean, isLast: Boolean): RoundedCornerShape {
    val large = 20.dp
    val small = 6.dp

    return when (isSelf) {
        true -> RoundedCornerShape(
            topStart = large,
            topEnd = if (isFirst) large else small,
            bottomEnd = if (isLast) large else small,
            bottomStart = large
        )
        false -> RoundedCornerShape(
            topStart = if (isFirst) large else small,
            topEnd = large,
            bottomEnd = large,
            bottomStart = if (isLast) large else small
        )
    }
}

@Composable
private fun InputBar(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    input: ConversationInput,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = input.isEnabled,
            placeholder = { Text(input.hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            shape = RoundedCornerShape(28.dp),
            maxLines = 5,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        FilledIconButton(
            onClick = onSend,
            enabled = input.isEnabled && value.text.isNotBlank()
        ) {
            Icon(painterResource(R.drawable.ic_symbol_send), contentDescription = stringResource(R.string.action_send))
        }
    }
}
