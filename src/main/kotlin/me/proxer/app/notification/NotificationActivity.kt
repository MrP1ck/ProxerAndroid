package me.proxer.app.notification

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.PagedList
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.showErrorSnackbar
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.ProxerNotification
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.intentFor
import me.proxer.app.util.extension.startActivity
import me.proxer.library.enums.NotificationType
import org.koin.androidx.compose.koinViewModel

/**
 * The notifications of the account, e.g. new forum posts or friend requests.
 *
 * @author Ruben Gees
 */
class NotificationActivity : ComposeActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<NotificationActivity>()
        fun getIntent(context: Context) = context.intentFor<NotificationActivity>()
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val context = LocalContext.current
        val viewModel = koinViewModel<NotificationViewModel>()
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)
        val deletionError by viewModel.deletionError.observeAsState()
        var isDeleteAllDialogVisible by rememberSaveable { mutableStateOf(false) }

        LifecycleResumeEffect(Unit) {
            AccountNotifications.cancel(context)

            onPauseOrDispose { }
        }

        ProxerScaffold(
            title = stringResource(R.string.section_notifications),
            onNavigateUp = navigator::navigateUp,
            actions = {
                if (!state.data.isNullOrEmpty()) {
                    IconButton(onClick = { isDeleteAllDialogVisible = true }) {
                        Icon(
                            painterResource(R.drawable.ic_symbol_delete),
                            contentDescription = stringResource(R.string.action_delete_all)
                        )
                    }
                }
            }
        ) { padding ->
            val snackbarHostState = LocalSnackbarHostState.current

            LaunchedEffect(deletionError) {
                deletionError?.let {
                    snackbarHostState?.showErrorSnackbar(
                        context = context,
                        error = it,
                        onAction = onErrorAction,
                        message = context.getString(R.string.error_notification_deletion, context.getString(it.message))
                    )
                }
            }

            PagedList(
                state = state,
                onLoadMore = viewModel::loadIfPossible,
                onRefresh = viewModel::refresh,
                onErrorAction = onErrorAction,
                contentPadding = padding,
                emptyMessage = R.string.error_no_data_notifications,
                key = { it.id }
            ) { notification ->
                NotificationItem(
                    notification = notification,
                    onClick = { navigator.showPage(notification.contentLink, skipCheck = true) },
                    onDelete = { viewModel.addItemToDelete(notification) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        if (isDeleteAllDialogVisible) {
            AlertDialog(
                onDismissRequest = { isDeleteAllDialogVisible = false },
                icon = { Icon(painterResource(R.drawable.ic_symbol_delete), contentDescription = null) },
                text = { Text(stringResource(R.string.dialog_notification_deletion_confirmation_content).trim()) },
                confirmButton = {
                    DialogButton(R.string.dialog_notification_deletion_confirmation_positive, onClick = {
                        isDeleteAllDialogVisible = false

                        viewModel.deleteAll()
                    })
                },
                dismissButton = { DialogButton(R.string.cancel, onClick = { isDeleteAllDialogVisible = false }) }
            )
        }
    }
}

@Composable
private fun NotificationItem(
    notification: ProxerNotification,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val text = remember(notification.text) { AnnotatedString.fromHtml(notification.text) }
    val date = remember(notification.date) { notification.date.distanceInWordsToNow(context) }

    ListItem(
        headlineContent = { Text(text) },
        supportingContent = { Text(date) },
        leadingContent = { Icon(painterResource(notification.type.icon()), contentDescription = null) },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_symbol_close),
                    contentDescription = stringResource(R.string.fragment_notification_delete_content_description)
                )
            }
        },
        modifier = modifier.clickable(onClick = onClick)
    )
}

private fun NotificationType.icon() = when (this) {
    NotificationType.BOARD_MESSAGE, NotificationType.BOARD_REPLY,
    NotificationType.FORUM_POST, NotificationType.FORUM_TOPIC -> R.drawable.ic_symbol_forum
    NotificationType.FRIEND_ACCEPT -> R.drawable.ic_symbol_person_add
    NotificationType.SUBS_PROJECT_STATE, NotificationType.APPS_RELEASE,
    NotificationType.APPS_STATE -> R.drawable.ic_symbol_new_releases
    NotificationType.REMINDER -> R.drawable.ic_symbol_bookmark
    NotificationType.TICKET, NotificationType.TICKET_COMMENT,
    NotificationType.TICKET_MENTION -> R.drawable.ic_symbol_support_agent
    NotificationType.GALLERY_ALBUM -> R.drawable.ic_symbol_image
    else -> R.drawable.ic_symbol_notifications
}
