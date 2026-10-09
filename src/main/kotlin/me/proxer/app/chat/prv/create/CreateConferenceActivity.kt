package me.proxer.app.chat.prv.create

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.PrvMessengerActivity
import me.proxer.app.comment.userImageUrl
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.UserAvatar
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.showErrorSnackbar
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.startActivity
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Creates a new conference: a chat with one other user or a group with a topic.
 *
 * @author Ruben Gees
 */
class CreateConferenceActivity : ComposeActivity() {

    companion object {
        private const val IS_GROUP_EXTRA = "is_group"
        private const val INITIAL_PARTICIPANT_EXTRA = "initial_participant"

        fun navigateTo(context: Activity, isGroup: Boolean = false, initialParticipant: Participant? = null) {
            context.startActivity<CreateConferenceActivity>(
                IS_GROUP_EXTRA to isGroup,
                INITIAL_PARTICIPANT_EXTRA to initialParticipant
            )
        }

        fun getIntent(context: Activity, isGroup: Boolean = false, initialParticipant: Participant? = null): Intent {
            return context.intentFor<CreateConferenceActivity>(
                IS_GROUP_EXTRA to isGroup,
                INITIAL_PARTICIPANT_EXTRA to initialParticipant
            )
        }
    }

    private val isGroup: Boolean
        get() = intent.getBooleanExtra(IS_GROUP_EXTRA, false)

    private val initialParticipant: Participant?
        get() = intent.getParcelableExtra(INITIAL_PARTICIPANT_EXTRA)

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val context = LocalContext.current
        val viewModel = koinViewModel<CreateConferenceViewModel>()
        val validators = koinInject<Validators>()
        val user by rememberCurrentUser()
        val isLoading by viewModel.isLoading.observeAsState(false)
        val result by viewModel.result.observeAsState()
        val error by viewModel.error.observeAsState()
        val onErrorAction = rememberErrorActionHandler { }

        var topic by rememberSaveable { mutableStateOf("") }
        var topicError by rememberSaveable { mutableStateOf<Int?>(null) }
        var participantInput by rememberSaveable { mutableStateOf("") }
        var participantError by rememberSaveable { mutableStateOf<Int?>(null) }
        var message by rememberSaveable { mutableStateOf("") }
        var participants by rememberSaveable(
            stateSaver = listSaver<List<Participant>, Participant>(save = { it }, restore = { it })
        ) {
            mutableStateOf(listOfNotNull(initialParticipant))
        }

        val canAddParticipant = isGroup || participants.isEmpty()
        val topicFocus = remember { FocusRequester() }
        val participantFocus = remember { FocusRequester() }
        val messageFocus = remember { FocusRequester() }

        ProxerScaffold(
            title = stringResource(if (isGroup) R.string.action_create_group else R.string.action_create_chat),
            onNavigateUp = navigator::navigateUp,
            scrollBehavior = null
        ) { padding ->
            val snackbarHostState = LocalSnackbarHostState.current
            val scope = rememberCoroutineScope()

            fun showError(action: ErrorUtils.ErrorAction) {
                scope.launch { snackbarHostState?.showErrorSnackbar(context, action, onErrorAction) }
            }

            fun addParticipant(): Boolean {
                val username = participantInput.trim()

                participantError = when {
                    username.isBlank() -> R.string.error_input_empty
                    participants.any { it.username.equals(username, ignoreCase = true) } ->
                        R.string.error_duplicate_participant
                    username.equals(user?.name, ignoreCase = true) -> R.string.error_self_participant
                    else -> null
                }

                if (participantError == null) {
                    participants = participants + Participant(username)
                    participantInput = ""
                }

                return participantError == null
            }

            fun send() {
                if (isLoading == true) return

                val trimmedTopic = topic.trim()
                val trimmedMessage = message.trim()

                try {
                    validators.validateLogin()
                } catch (error: Exception) {
                    showError(ErrorUtils.handle(error))

                    return
                }

                when {
                    isGroup && trimmedTopic.isBlank() -> topicError = R.string.error_input_empty
                    trimmedMessage.isBlank() -> showError(ErrorUtils.ErrorAction(R.string.error_missing_message, ACTION_MESSAGE_HIDE))
                    participants.isEmpty() -> showError(ErrorUtils.ErrorAction(R.string.error_missing_participants, ACTION_MESSAGE_HIDE))
                    isGroup -> viewModel.createGroup(trimmedTopic, trimmedMessage, participants)
                    else -> viewModel.createChat(trimmedMessage, participants.first())
                }
            }

            LaunchedEffect(error) { error?.let { showError(it) } }

            LaunchedEffect(result) {
                result?.let {
                    finish()

                    PrvMessengerActivity.navigateTo(this@CreateConferenceActivity, it)
                }
            }

            LaunchedEffect(Unit) {
                when {
                    isGroup -> topicFocus.requestFocus()
                    participants.isEmpty() -> participantFocus.requestFocus()
                    else -> messageFocus.requestFocus()
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
                    .imePadding()
            ) {
                if (isLoading == true) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 8.dp)) {
                    if (isGroup) {
                        item(key = "topic") {
                            OutlinedTextField(
                                value = topic,
                                onValueChange = {
                                    topic = it
                                    topicError = null
                                },
                                label = { Text(stringResource(R.string.fragment_create_conference_topic_hint)) },
                                isError = topicError != null,
                                supportingText = topicError?.let { { Text(stringResource(it)) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        if (participants.isEmpty()) participantFocus.requestFocus() else messageFocus.requestFocus()
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .focusRequester(topicFocus)
                            )
                        }
                    }

                    item(key = "participantsHeader") {
                        SectionHeader(stringResource(R.string.fragment_create_conference_participants))
                    }

                    items(participants, key = { it.username }) { participant ->
                        ListItem(
                            headlineContent = { Text(participant.username) },
                            leadingContent = { UserAvatar(userImageUrl(participant.image)) },
                            trailingContent = {
                                IconButton(onClick = { participants = participants - participant }) {
                                    Icon(
                                        painterResource(R.drawable.ic_symbol_close),
                                        contentDescription = stringResource(
                                            R.string.fragment_create_conference_delete_description
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.animateItem()
                        )
                    }

                    if (canAddParticipant) {
                        item(key = "addParticipant") {
                            OutlinedTextField(
                                value = participantInput,
                                onValueChange = {
                                    participantInput = it
                                    participantError = null
                                },
                                label = { Text(stringResource(R.string.fragment_create_conference_add_participant)) },
                                placeholder = { Text(stringResource(R.string.fragment_create_conference_add_participant_hint)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_person_add), null) },
                                trailingIcon = {
                                    IconButton(onClick = { if (addParticipant() && !isGroup) messageFocus.requestFocus() }) {
                                        Icon(
                                            painterResource(R.drawable.ic_symbol_check),
                                            contentDescription = stringResource(
                                                R.string.fragment_create_conference_accept_description
                                            )
                                        )
                                    }
                                },
                                isError = participantError != null,
                                supportingText = participantError?.let { { Text(stringResource(it)) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { if (addParticipant()) messageFocus.requestFocus() }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .focusRequester(participantFocus)
                                    .animateItem()
                            )
                        }
                    }
                }

                Surface(tonalElevation = 2.dp) {
                    Column {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                placeholder = { Text(stringResource(R.string.fragment_messenger_message)) },
                                shape = RoundedCornerShape(28.dp),
                                maxLines = 5,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(messageFocus)
                            )

                            Spacer(Modifier.width(8.dp))

                            FilledIconButton(onClick = ::send, enabled = isLoading != true && message.isNotBlank()) {
                                Icon(
                                    painterResource(R.drawable.ic_symbol_send),
                                    contentDescription = stringResource(R.string.action_send)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
