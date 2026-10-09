package me.proxer.app.media.discussion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.proxer.app.R
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.distanceInWordsToNow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The forum threads about an entry.
 */
@Composable
fun DiscussionTab(
    entryId: String,
    contentPadding: PaddingValues,
    viewModel: DiscussionViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_discussions
    ) { discussions ->
        LazyColumn(contentPadding = contentPadding) {
            items(discussions, key = { it.id }) { discussion ->
                ListItem(
                    headlineContent = { Text(discussion.subject, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.fragment_discussion_meta_info,
                                discussion.firstPostUsername,
                                discussion.categoryName
                            ) + " · " + discussion.lastPostDate.distanceInWordsToNow(context)
                        )
                    },
                    leadingContent = { Icon(painterResource(R.drawable.ic_symbol_forum), contentDescription = null) },
                    trailingContent = { Text(discussion.postAmount.toString()) },
                    modifier = Modifier.clickable {
                        navigator.openTopic(discussion.id, discussion.categoryId, discussion.subject)
                    }
                )

                HorizontalDivider()
            }
        }
    }
}
