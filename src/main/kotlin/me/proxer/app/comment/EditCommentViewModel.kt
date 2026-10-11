package me.proxer.app.comment

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.data.Events
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.toLocalComment
import me.proxer.library.ProxerException
import me.proxer.library.ProxerException.ServerErrorType
import me.proxer.library.entity.info.RatingDetails
import me.proxer.library.enums.UserMediaProgress

/**
 * @author Ruben Gees
 */
class EditCommentViewModel(
    dependencies: ViewModelDependencies,
    private val id: String?,
    private val entryId: String?
) : BaseViewModel<LocalComment>(dependencies) {

    override val isLoginRequired = true

    companion object {
        private const val MAX_LENGTH = 20_000

        private val defaultComment = LocalComment(
            id = "",
            entryId = "",
            mediaProgress = UserMediaProgress.WATCHED,
            ratingDetails = RatingDetails(genre = 0, story = 0, animation = 0, characters = 0, music = 0),
            content = "",
            overallRating = 0,
            episode = 0
        )
    }

    init {
        require(id != null || entryId != null) {
            "Either id or entryId must be not null"
        }
    }

    override suspend fun loadData(): LocalComment {
        validate()

        val comment = try {
            api.comment.comment(id, entryId).await().toLocalComment()
        } catch (error: ProxerException) {
            if (error.isInvalidCommentError) defaultComment else throw error
        }

        isUpdate.value = comment.id.isNotBlank() && comment.content.isNotBlank()

        val safeEntryId = entryId

        return if (safeEntryId != null && !isUpdate.value) {
            storageHelper.getCommentDraft(safeEntryId)?.let { draft -> comment.copy(content = draft) } ?: comment
        } else {
            comment
        }
    }

    val isUpdate = MutableStateFlow(id.isNullOrBlank().not())
    val publishResult = Events<LocalComment>()
    val publishError = Events<ErrorUtils.ErrorAction>()

    private var publishJob: Job? = null

    fun updateRating(rating: Float) {
        require(rating in 0.0..5.0)

        data.value = data.value?.copy(overallRating = (rating * 2).toInt())
    }

    fun updateContent(newContent: String) {
        data.value = data.value?.copy(content = newContent)
    }

    fun publish() {
        val comment = data.value

        if (comment == null || comment.content.isBlank() && comment.overallRating == 0) return

        val isCreatable = comment.id.isNotEmpty() || entryId != null
        val isValid = comment.content.length <= MAX_LENGTH && comment.mediaProgress != UserMediaProgress.WILL_WATCH

        if (isValid && !isCreatable) return

        publishJob?.cancel()
        publishJob = viewModelScope.launch {
            isLoading.value = true

            catchingErrors {
                validate()

                when {
                    comment.content.length > MAX_LENGTH -> throw CommentTooLongException()
                    comment.mediaProgress == UserMediaProgress.WILL_WATCH -> throw CommentInvalidProgressException()
                    comment.id.isNotEmpty() ->
                        api.comment.update(comment.id)
                            .comment(comment.content.trim())
                            .rating(comment.overallRating)
                            .awaitNullable()
                    else ->
                        api.comment.create(requireNotNull(entryId))
                            .comment(comment.content.trim())
                            .rating(comment.overallRating)
                            .awaitNullable()
                }
            }
                .onSuccess {
                    publishResult.send(data.value ?: comment)
                    data.value = null
                }
                .onFailure { publishError.send(ErrorUtils.handle(it)) }

            if (isActive) isLoading.value = false
        }
    }

    private val Throwable.isInvalidCommentError
        get() = this is ProxerException && serverErrorType == ServerErrorType.COMMENT_INVALID_COMMENT
}
