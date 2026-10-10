package me.proxer.app.chat.prv

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.ConferenceList
import me.proxer.app.chat.ConversationInput
import me.proxer.app.chat.ConversationMessage
import me.proxer.app.chat.ConversationScreen
import me.proxer.app.chat.ReportMessageDialog
import me.proxer.app.chat.prv.conference.ConferenceViewModel
import me.proxer.app.chat.prv.conference.info.ConferenceInfoActivity
import me.proxer.app.chat.prv.message.MessengerFragmentPingEvent
import me.proxer.app.chat.prv.message.MessengerReportViewModel
import me.proxer.app.chat.prv.message.MessengerViewModel
import me.proxer.app.chat.prv.sync.MessengerDao
import me.proxer.app.chat.prv.sync.MessengerNotifications
import me.proxer.app.ui.components.LiveDataEffect
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.RegisterWhileResumed
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.map
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toDate
import me.proxer.library.enums.MessageAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * A conference of the messenger. Without a conference (when sharing text to the app), the conferences are shown to
 * choose one.
 *
 * @author Ruben Gees
 */
class PrvMessengerActivity : ComposeActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference, initialMessage: String? = null) {
            context.startActivity<PrvMessengerActivity>(
                CONFERENCE_EXTRA to conference,
                Intent.EXTRA_TEXT to initialMessage
            )
        }

        fun getIntent(context: Context, conference: LocalConference, initialMessage: String? = null): Intent {
            return context.intentFor<PrvMessengerActivity>(
                CONFERENCE_EXTRA to conference,
                Intent.EXTRA_TEXT to initialMessage
            )
        }

        fun getIntent(context: Context, conferenceId: String, initialMessage: String? = null): Intent {
            return context.intentFor<PrvMessengerActivity>(
                ShortcutManagerCompat.EXTRA_SHORTCUT_ID to conferenceId,
                Intent.EXTRA_TEXT to initialMessage
            )
        }
    }

    @Composable
    override fun Content() {
        val messengerDao = koinInject<MessengerDao>()
        val initialMessage = intent.getStringExtra(Intent.EXTRA_TEXT)
        val passedConference = intent.getParcelableExtra<LocalConference>(CONFERENCE_EXTRA)
        val shortcutConferenceId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)?.toLongOrNull()

        when {
            passedConference != null -> MessengerScreen(passedConference, initialMessage)
            shortcutConferenceId != null -> {
                val conference by produceState<LocalConference?>(null, shortcutConferenceId) {
                    value = withContext(Dispatchers.IO) { messengerDao.findConference(shortcutConferenceId) }

                    if (value == null) finish()
                }

                when (val safeConference = conference) {
                    null -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    else -> MessengerScreen(safeConference, initialMessage)
                }
            }
            else -> ShareTargetScreen(initialMessage)
        }
    }

    @Composable
    private fun MessengerScreen(initialConference: LocalConference, initialMessage: String?) {
        val navigator = LocalAppNavigator.current
        val context = LocalContext.current
        val viewModel = koinViewModel<MessengerViewModel> { parametersOf(initialConference) }
        val user by rememberCurrentUser()
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)
        val conference by viewModel.conference.observeAsState(initialConference)
        var loadedDraft by remember { mutableStateOf<String?>(null) }
        val deleted by viewModel.deleted.observeAsState()
        var isReportDialogVisible by rememberSaveable { mutableStateOf(false) }

        RegisterWhileResumed(MessengerFragmentPingEvent::class.java)

        LiveDataEffect(viewModel.draft) { loadedDraft = it }
        LaunchedEffect(Unit) { if (initialMessage == null) viewModel.loadDraft() }
        LaunchedEffect(deleted) { if (deleted != null) finish() }

        LifecycleResumeEffect(Unit) {
            MessengerNotifications.cancel(context)

            onPauseOrDispose { }
        }

        val messages = remember(state, user, conference.isGroup) {
            state.map { messages ->
                messages.map { message ->
                    ConversationMessage(
                        id = message.id.toString(),
                        userId = message.userId,
                        username = message.username,
                        imageUrl = null,
                        text = message.message,
                        content = message.styledMessage,
                        actionText = when (message.action) {
                            MessageAction.NONE -> null
                            else -> message.action.toAppString(context, message.username, message.message)
                        },
                        date = message.date.toDate(),
                        isPending = message.id < 0,
                        isSelf = message.userId == user?.id
                    )
                }
            }
        }

        val openInfo = {
            when (conference.isGroup) {
                true -> ConferenceInfoActivity.navigateTo(this, conference)
                false -> navigator.openProfile(null, conference.topic, conference.image)
            }
        }

        ConversationScreen(
            title = conference.topic,
            subtitle = null,
            state = messages,
            input = ConversationInput(
                isEnabled = state.data != null && user != null,
                hint = stringResource(
                    when (state.data == null) {
                        true -> R.string.fragment_chat_loading_message
                        false -> R.string.fragment_messenger_message
                    }
                )
            ),
            initialDraft = initialMessage ?: loadedDraft,
            showAuthors = conference.isGroup,
            onNavigateUp = navigator::navigateUp,
            onErrorAction = onErrorAction,
            onLoadMore = viewModel::loadIfPossible,
            onDraftChange = viewModel::updateDraft,
            onSend = viewModel::sendMessage,
            onAuthorClick = { navigator.openProfile(it.userId, it.username) },
            onReport = null,
            emptyMessage = R.string.error_no_data_chat,
            onTitleClick = openInfo,
            actions = {
                IconButton(onClick = openInfo) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_info),
                        contentDescription = stringResource(R.string.section_info)
                    )
                }

                OverflowMenu(onReport = { isReportDialogVisible = true })
            }
        )

        if (isReportDialogVisible) {
            ReportMessageDialog(
                viewModel = koinViewModel<MessengerReportViewModel>(),
                id = conference.id.toString(),
                onDismiss = { isReportDialogVisible = false }
            )
        }
    }

    @Composable
    private fun ShareTargetScreen(initialMessage: String?) {
        val navigator = LocalAppNavigator.current
        val viewModel = koinViewModel<ConferenceViewModel> { parametersOf("") }
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)

        ProxerScaffold(
            title = stringResource(R.string.activity_prv_messenger_send_to),
            onNavigateUp = navigator::navigateUp
        ) { padding ->
            ConferenceList(
                state = state,
                isSearching = false,
                onErrorAction = onErrorAction,
                onConferenceClick = {
                    navigateTo(this, it.conference, initialMessage)
                    finish()
                },
                contentPadding = padding
            )
        }
    }
}

@Composable
private fun OverflowMenu(onReport: () -> Unit) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                painterResource(R.drawable.ic_symbol_more_vert),
                contentDescription = stringResource(R.string.media_list_options)
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_report)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_flag), contentDescription = null) },
                onClick = {
                    isExpanded = false
                    onReport()
                }
            )
        }
    }
}
