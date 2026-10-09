package me.proxer.app.comment

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import me.proxer.app.R
import me.proxer.app.ui.components.BBCodeText
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.RatingStars
import me.proxer.app.ui.view.bbcode.BBTree
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

/**
 * The data of a comment shown by [CommentCard]. Media comments show the author, comments on a profile show the entry.
 */
data class CommentCardData(
    val id: String,
    val title: String,
    val imageUrl: HttpUrl?,
    val subtitle: String,
    val time: String,
    val overallRating: Int,
    val ratingDetails: RatingDetails,
    val content: BBTree,
    val authorId: String,
    val helpfulVotes: Int,
    val isOwn: Boolean
)

/**
 * A comment with the ratings, the content and, for the own comments, actions to edit and delete it.
 */
@Composable
fun CommentCard(
    comment: CommentCardData,
    onHeaderClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var areRatingDetailsVisible by rememberSaveable(comment.id) { mutableStateOf(false) }
    val hasRatingDetails = comment.ratingDetails.let {
        it.genre + it.story + it.animation + it.characters + it.music > 0
    }

    Card(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onHeaderClick)
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                if (comment.imageUrl != null) {
                    ProxerAsyncImage(comment.imageUrl, null, Modifier.size(40.dp), showErrorIcon = false)
                } else {
                    Icon(
                        painterResource(R.drawable.ic_symbol_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    comment.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${comment.subtitle} · ${comment.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (comment.isOwn) {
                OwnCommentMenu(onEdit, onDelete)
            }
        }

        if (comment.overallRating > 0 || hasRatingDetails) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (hasRatingDetails) it.clickable { areRatingDetailsVisible = !areRatingDetailsVisible } else it }
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RatingStars(comment.overallRating / 2f)

                if (hasRatingDetails) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_expand_more),
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (areRatingDetailsVisible) 180f else 0f)
                    )
                }
            }

            AnimatedVisibility(areRatingDetailsVisible) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    RatingDetailRow(R.string.fragment_comment_rating_genre, comment.ratingDetails.genre)
                    RatingDetailRow(R.string.fragment_comment_rating_story, comment.ratingDetails.story)
                    RatingDetailRow(R.string.fragment_comment_rating_animation, comment.ratingDetails.animation)
                    RatingDetailRow(R.string.fragment_comment_rating_characters, comment.ratingDetails.characters)
                    RatingDetailRow(R.string.fragment_comment_rating_music, comment.ratingDetails.music)
                }
            }
        }

        if (!comment.content.isBlank()) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            BBCodeText(
                tree = comment.content,
                userId = comment.authorId,
                collapsedHeight = 240.dp,
                key = comment.id,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(painterResource(R.drawable.ic_symbol_thumb_up), contentDescription = null, modifier = Modifier.size(16.dp))

            Text(comment.helpfulVotes.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RatingDetailRow(@StringRes title: Int, rating: Int) {
    if (rating <= 0) return

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(128.dp)
        )

        RatingStars(rating.toFloat(), starSize = 14.dp)
    }
}

@Composable
private fun OwnCommentMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var isMenuVisible by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isMenuVisible = true }) {
            Icon(painterResource(R.drawable.ic_symbol_more_vert), contentDescription = stringResource(R.string.media_list_options))
        }

        DropdownMenu(expanded = isMenuVisible, onDismissRequest = { isMenuVisible = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.action_edit)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_edit), contentDescription = null) },
                onClick = {
                    isMenuVisible = false
                    onEdit()
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.dialog_comment_delete_positive)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_delete), contentDescription = null) },
                onClick = {
                    isMenuVisible = false
                    onDelete()
                }
            )
        }
    }
}

/**
 * Asks the user to confirm deleting the comment of [author].
 */
@Composable
fun DeleteCommentDialog(author: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val message = stringResource(R.string.dialog_comment_delete_message, author)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_symbol_delete), contentDescription = null) },
        text = { Text(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    onConfirm()
                }
            ) {
                Text(stringResource(R.string.dialog_comment_delete_positive))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

fun userImageUrl(image: String) = if (image.isBlank()) null else ProxerUrls.userImage(image)
