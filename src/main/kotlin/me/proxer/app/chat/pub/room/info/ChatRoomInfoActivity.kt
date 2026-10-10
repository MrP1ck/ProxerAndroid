package me.proxer.app.chat.pub.room.info

import android.app.Activity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.UserListItem
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The active users of a room of the public chat. Moderators are shown first.
 *
 * @author Ruben Gees
 */
class ChatRoomInfoActivity : ComposeActivity() {

    companion object {
        private const val CHAT_ROOM_ID_EXTRA = "chat_room_id"
        private const val CHAT_ROOM_NAME_EXTRA = "chat_room_name"

        fun navigateTo(context: Activity, chatRoomId: String, chatRoomName: String) {
            context.startActivity<ChatRoomInfoActivity>(
                CHAT_ROOM_ID_EXTRA to chatRoomId,
                CHAT_ROOM_NAME_EXTRA to chatRoomName
            )
        }
    }

    private val chatRoomId: String
        get() = intent.getSafeStringExtra(CHAT_ROOM_ID_EXTRA)

    private val chatRoomName: String
        get() = intent.getSafeStringExtra(CHAT_ROOM_NAME_EXTRA)

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val viewModel = koinViewModel<ChatRoomInfoViewModel> { parametersOf(chatRoomId) }
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)

        LifecycleResumeEffect(viewModel) {
            viewModel.resumePolling()

            onPauseOrDispose { viewModel.pausePolling() }
        }

        ProxerScaffold(title = chatRoomName, onNavigateUp = navigator::navigateUp) { padding ->
            ContentStateHost(state, onErrorAction, contentPadding = padding) { users ->
                LazyColumn(contentPadding = padding) {
                    item(key = "header") {
                        SectionHeader(stringResource(R.string.fragment_chat_room_active_users_title))
                    }

                    items(users, key = { it.id }) { user ->
                        UserListItem(
                            username = user.name,
                            image = user.image,
                            status = user.status,
                            badge = R.drawable.ic_symbol_shield_person.takeIf { user.isModerator },
                            onClick = { navigator.openProfile(user.id, user.name, user.image) }
                        )
                    }
                }
            }
        }
    }
}
