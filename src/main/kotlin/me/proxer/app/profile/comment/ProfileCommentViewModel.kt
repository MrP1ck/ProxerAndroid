package me.proxer.app.profile.comment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proxer.app.base.PagedViewModel
import me.proxer.app.comment.LocalComment
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.toParsedUserComment
import me.proxer.library.enums.Category
import me.proxer.library.enums.CommentContentType
import java.time.Instant
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ProfileCommentViewModel(
    private val userId: String?,
    private val username: String?,
    category: Category?
) : PagedViewModel<ParsedUserComment>() {

    override val itemsOnPage = 10

    override suspend fun loadData(): List<ParsedUserComment> {
        validate()

        val comments = api.user.comments(userId, username)
            .category(category)
            .page(page)
            .limit(itemsOnPage)
            .hasContent(CommentContentType.COMMENT, CommentContentType.RATING)
            .await()

        return withContext(Dispatchers.Default) { comments.map { comment -> comment.toParsedUserComment() } }
    }

    var category by Delegates.observable(category) { _, old, new ->
        if (old != new) reload()
    }

    val itemDeletionError = Events<ErrorUtils.ErrorAction>()

    private var deleteJob: Job? = null

    fun deleteComment(comment: ParsedUserComment) {
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            catchingErrors {
                api.comment.update(comment.id)
                    .comment("")
                    .rating(0)
                    .awaitNullable()

                storageHelper.deleteCommentDraft(comment.entryId)
            }
                .onSuccess { data.value = data.value?.filter { it.id != comment.id } }
                .onFailure { itemDeletionError.send(ErrorUtils.handle(it)) }
        }
    }

    fun updateComment(comment: LocalComment) {
        data.value = data.value?.map {
            if (it.id == comment.id) {
                it.copy(
                    ratingDetails = comment.ratingDetails,
                    parsedContent = comment.parsedContent,
                    overallRating = comment.overallRating,
                    instant = Instant.now()
                )
            } else {
                it
            }
        }
    }
}
