package me.proxer.app.profile.comment

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.comment.CommentCard
import me.proxer.app.comment.CommentCardData
import me.proxer.app.comment.DeleteCommentDialog
import me.proxer.app.comment.EditCommentActivity
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.PagedList
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The comments a user wrote, filterable by anime and manga.
 */
@Composable
fun ProfileCommentsTab(
    userId: String?,
    username: String?,
    contentPadding: PaddingValues,
    viewModel: ProfileCommentViewModel = koinViewModel(parameters = { parametersOf(userId, username, null) })
) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val user by rememberCurrentUser()
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)
    val deletionError by viewModel.itemDeletionError.observeAsState()

    var category by rememberSaveable { mutableStateOf(viewModel.category) }
    var commentToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val editComment = rememberLauncherForActivityResult(EditCommentActivity.Contract()) { comment ->
        if (comment != null) viewModel.updateComment(comment)
    }

    LaunchedEffect(deletionError) {
        deletionError?.let {
            snackbarHostState?.showSnackbar(
                context.getString(R.string.error_comment_deletion, context.getString(it.message))
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                null to R.string.action_filter_all,
                Category.ANIME to R.string.action_filter_anime,
                Category.MANGA to R.string.action_filter_manga
            ).forEach { (filterCategory, label) ->
                FilterChip(
                    selected = category == filterCategory,
                    onClick = {
                        category = filterCategory
                        viewModel.category = filterCategory
                    },
                    label = { Text(stringResource(label)) }
                )
            }
        }

        PagedList(
            state = state,
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()).plus(
                PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            emptyMessage = R.string.error_no_data_comments,
            key = { it.id }
        ) { comment ->
            CommentCard(
                comment = CommentCardData(
                    id = comment.id,
                    title = comment.entryName,
                    imageUrl = ProxerUrls.entryImage(comment.entryId),
                    subtitle = comment.mediaProgress.toEpisodeAppString(context, comment.episode, comment.category),
                    time = comment.date.distanceInWordsToNow(context),
                    overallRating = comment.overallRating,
                    ratingDetails = comment.ratingDetails,
                    content = comment.parsedContent,
                    authorId = comment.authorId,
                    helpfulVotes = comment.helpfulVotes,
                    isOwn = comment.authorId == user?.id
                ),
                onHeaderClick = { navigator.openMedia(comment.entryId, comment.entryName, comment.category) },
                onEdit = {
                    editComment.launch(
                        EditCommentActivity.Contract.Input(comment.id, comment.entryId, comment.entryName)
                    )
                },
                onDelete = { commentToDelete = comment.id }
            )
        }
    }

    commentToDelete?.let { id ->
        val comment = state.data?.find { it.id == id }

        if (comment != null) {
            DeleteCommentDialog(
                author = comment.author,
                onConfirm = { viewModel.deleteComment(comment) },
                onDismiss = { commentToDelete = null }
            )
        }
    }
}
