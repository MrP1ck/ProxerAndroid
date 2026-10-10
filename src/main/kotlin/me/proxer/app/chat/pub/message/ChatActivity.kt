package me.proxer.app.chat.pub.message

import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.ConversationInput
import me.proxer.app.chat.ConversationMessage
import me.proxer.app.chat.ConversationScreen
import me.proxer.app.chat.ReportMessageDialog
import me.proxer.app.chat.pub.room.info.ChatRoomInfoActivity
import me.proxer.app.comment.userImageUrl
import me.proxer.app.ui.components.LiveDataEffect
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.map
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * A room of the public chat.
 *
 * @author Ruben Gees
 */
class ChatActivity : ComposeActivity() {

    companion object {
        private const val CHAT_ROOM_ID_EXTRA = "chat_room_id"
        private const val CHAT_ROOM_NAME_EXTRA = "chat_room_name"
        private const val CHAT_ROOM_IS_READ_ONLY_EXTRA = "chat_room_is_read_only"

        fun navigateTo(context: Activity, chatRoomId: String, chatRoomName: String, chatRoomIsReadOnly: Boolean) {
            context.startActivity<ChatActivity>(
                CHAT_ROOM_ID_EXTRA to chatRoomId,
                CHAT_ROOM_NAME_EXTRA to chatRoomName,
                CHAT_ROOM_IS_READ_ONLY_EXTRA to chatRoomIsReadOnly
            )
        }
    }

    private val chatRoomId: String
        get() = intent.getSafeStringExtra(CHAT_ROOM_ID_EXTRA)

    private val chatRoomName: String
        get() = intent.getSafeStringExtra(CHAT_ROOM_NAME_EXTRA)

    private val isReadOnly: Boolean
        get() = intent.getBooleanExtra(CHAT_ROOM_IS_READ_ONLY_EXTRA, false)

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current
        val viewModel = koinViewModel<ChatViewModel> { parametersOf(chatRoomId) }
        val user by rememberCurrentUser()
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)
        var loadedDraft by remember { mutableStateOf<String?>(null) }
        var messageToReport by rememberSaveable { mutableStateOf<String?>(null) }

        LiveDataEffect(viewModel.draft) { loadedDraft = it }
        LaunchedEffect(Unit) { viewModel.loadDraft() }

        LifecycleResumeEffect(viewModel) {
            viewModel.resumePolling()

            onPauseOrDispose { viewModel.pausePolling() }
        }

        LiveDataEffect(viewModel.sendMessageError) {
            snackbarHostState?.showSnackbar(
                context.getString(R.string.error_chat_send_message, context.getString(it.message))
            )
        }

        val messages = remember(state, user) {
            state.map { messages ->
                messages.map { message ->
                    ConversationMessage(
                        id = message.id,
                        userId = message.userId,
                        username = message.username,
                        imageUrl = userImageUrl(message.image),
                        text = message.message,
                        content = message.styledMessage,
                        actionText = null,
                        date = message.date,
                        isPending = message.id.toLong() < 0,
                        isSelf = message.userId == user?.id
                    )
                }
            }
        }

        ConversationScreen(
            title = chatRoomName,
            subtitle = null,
            state = messages,
            input = ConversationInput(
                isEnabled = state.data != null && !isReadOnly && user != null,
                hint = stringResource(
                    when {
                        state.data == null -> R.string.fragment_chat_loading_message
                        isReadOnly -> R.string.fragment_chat_read_only_message
                        user == null -> R.string.fragment_chat_login_required_message
                        else -> R.string.fragment_messenger_message
                    }
                )
            ),
            initialDraft = loadedDraft,
            showAuthors = true,
            onNavigateUp = navigator::navigateUp,
            onErrorAction = onErrorAction,
            onLoadMore = viewModel::loadIfPossible,
            onDraftChange = viewModel::updateDraft,
            onSend = viewModel::sendMessage,
            onAuthorClick = { navigator.openProfile(it.userId, it.username) },
            onReport = if (user != null) {
                { messageToReport = it.id }
            } else {
                null
            },
            onTitleClick = { ChatRoomInfoActivity.navigateTo(this, chatRoomId, chatRoomName) },
            actions = {
                IconButton(onClick = { ChatRoomInfoActivity.navigateTo(this@ChatActivity, chatRoomId, chatRoomName) }) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_info),
                        contentDescription = stringResource(R.string.section_info)
                    )
                }
            }
        )

        messageToReport?.let { id ->
            ReportMessageDialog(
                viewModel = koinViewModel<ChatReportViewModel>(),
                id = id,
                onDismiss = { messageToReport = null }
            )
        }
    }
}
