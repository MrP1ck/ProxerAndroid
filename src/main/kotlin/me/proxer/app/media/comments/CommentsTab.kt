package me.proxer.app.media.comments

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.comment.CommentCard
import me.proxer.app.comment.CommentCardData
import me.proxer.app.comment.DeleteCommentDialog
import me.proxer.app.comment.EditCommentActivity
import me.proxer.app.comment.userImageUrl
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
import me.proxer.library.enums.CommentSortCriteria
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The comments on an entry, sortable by helpfulness or date. The user can write, edit and delete their comment.
 */
@Composable
fun CommentsTab(
    entryId: String,
    name: String?,
    category: Category?,
    contentPadding: PaddingValues,
    viewModel: CommentsViewModel = koinViewModel(parameters = { parametersOf(entryId, CommentSortCriteria.RATING) })
) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val user by rememberCurrentUser()
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)
    val deletionError by viewModel.itemDeletionError.observeAsState()

    var sortCriteria by rememberSaveable { mutableStateOf(viewModel.sortCriteria) }
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

    Box(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    CommentSortCriteria.RATING to R.string.comments_sort_helpful,
                    CommentSortCriteria.TIME to R.string.comments_sort_newest
                ).forEach { (criteria, label) ->
                    FilterChip(
                        selected = sortCriteria == criteria,
                        onClick = {
                            sortCriteria = criteria
                            viewModel.sortCriteria = criteria
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
                    PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp)
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                emptyMessage = R.string.error_no_data_comments,
                key = { it.id }
            ) { comment ->
                CommentCard(
                    comment = CommentCardData(
                        id = comment.id,
                        title = comment.author,
                        imageUrl = userImageUrl(comment.image),
                        subtitle = comment.mediaProgress.toEpisodeAppString(
                            context,
                            comment.episode,
                            category ?: Category.ANIME
                        ),
                        time = comment.date.distanceInWordsToNow(context),
                        overallRating = comment.overallRating,
                        ratingDetails = comment.ratingDetails,
                        content = comment.parsedContent,
                        authorId = comment.authorId,
                        helpfulVotes = comment.helpfulVotes,
                        isOwn = comment.authorId == user?.id
                    ),
                    onHeaderClick = { navigator.openProfile(comment.authorId, comment.author, comment.image) },
                    onEdit = {
                        editComment.launch(
                            EditCommentActivity.Contract.Input(comment.id, comment.entryId, name)
                        )
                    },
                    onDelete = { commentToDelete = comment.id }
                )
            }
        }

        if (user != null && state.data != null) {
            ExtendedFloatingActionButton(
                onClick = { editComment.launch(EditCommentActivity.Contract.Input(entryId = entryId, name = name)) },
                icon = { Icon(painterResource(R.drawable.ic_symbol_edit), contentDescription = null) },
                text = { Text(stringResource(R.string.action_write_comment)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = contentPadding.calculateBottomPadding())
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
