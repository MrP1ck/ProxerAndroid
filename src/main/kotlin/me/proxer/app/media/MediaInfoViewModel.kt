package me.proxer.app.media

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.exception.AgeConfirmationRequiredException
import me.proxer.app.exception.NotLoggedInException
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.data.Events
import me.proxer.app.util.data.send
import me.proxer.app.util.extension.await
import me.proxer.app.util.extension.awaitNullable
import me.proxer.app.util.extension.catchingErrors
import me.proxer.app.util.extension.isTrulyAgeRestricted
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.MediaUserInfo

/**
 * @author Ruben Gees
 */
class MediaInfoViewModel(
    dependencies: ViewModelDependencies,
    private val entryId: String
) : BaseViewModel<Entry>(dependencies) {

    override suspend fun loadData(): Entry {
        validate()

        val entry = api.info.entry(entryId).await()

        if (entry.isTrulyAgeRestricted) {
            if (!storageHelper.isLoggedIn) {
                throw NotLoggedInException()
            } else if (!preferenceHelper.isAgeRestrictedMediaAllowed) {
                throw AgeConfirmationRequiredException()
            }
        }

        if (storageHelper.isLoggedIn) {
            userInfoJob = viewModelScope.launch {
                catchingErrors { api.info.userInfo(entryId).await() }
                    .onSuccess { userInfoData.value = it }
            }
        }

        return entry
    }

    val userInfoData = MutableStateFlow<MediaUserInfo?>(null)

    val userInfoUpdateData = Events<Unit>()
    val userInfoUpdateError = Events<ErrorAction>()

    private var userInfoJob: Job? = null
    private var userInfoUpdateJob: Job? = null

    init {
        viewModelScope.launch {
            storageHelper.isLoggedInChanges.collect {
                if (it && error.value?.buttonAction == ButtonAction.LOGIN) {
                    reload()
                }
            }
        }

        viewModelScope.launch {
            preferenceHelper.isAgeRestrictedMediaAllowedChanges.collect {
                if (error.value?.buttonAction == ButtonAction.AGE_CONFIRMATION) {
                    reload()
                }
            }
        }
    }

    fun note() = updateUserInfo(UserInfoUpdateType.NOTE)

    fun markAsFinished() = updateUserInfo(UserInfoUpdateType.FINISHED)

    fun toggleFavorite() {
        if (userInfoData.value?.isTopTen == true) {
            updateUserInfo(UserInfoUpdateType.UNFAVORITE)
        } else {
            updateUserInfo(UserInfoUpdateType.FAVORITE)
        }
    }

    fun toggleSubscription() {
        if (userInfoData.value?.isSubscribed == true) {
            updateUserInfo(UserInfoUpdateType.UNSUBSCRIBE)
        } else {
            updateUserInfo(UserInfoUpdateType.SUBSCRIBE)
        }
    }

    private fun updateUserInfo(updateType: UserInfoUpdateType) {
        userInfoJob?.cancel()
        userInfoUpdateJob?.cancel()
        userInfoUpdateJob = viewModelScope.launch {
            catchingErrors {
                validators.validateLogin()

                sendUpdate(updateType)
            }
                .onSuccess {
                    userInfoUpdateData.send()
                    userInfoData.value?.also { userInfoData.value = applyMediaUserInfoChanges(it, updateType) }
                }
                .onFailure { userInfoUpdateError.send(ErrorUtils.handle(it)) }
        }
    }

    private suspend fun sendUpdate(updateType: UserInfoUpdateType) {
        when (updateType) {
            UserInfoUpdateType.NOTE -> api.info.note(entryId).awaitNullable()
            UserInfoUpdateType.FAVORITE -> api.info.markAsFavorite(entryId).awaitNullable()
            UserInfoUpdateType.FINISHED -> api.info.markAsFinished(entryId).awaitNullable()
            UserInfoUpdateType.SUBSCRIBE -> api.info.subscribe(entryId).awaitNullable()
            UserInfoUpdateType.UNSUBSCRIBE -> api.ucp.deleteSubscription(entryId).awaitNullable()
            UserInfoUpdateType.UNFAVORITE -> {
                val topTenId = api.ucp.topTen().await().find { topTenEntry -> topTenEntry.entryId == entryId }?.id

                if (topTenId != null) {
                    api.ucp.deleteFavorite(topTenId).awaitNullable()
                }
            }
        }
    }

    private fun applyMediaUserInfoChanges(data: MediaUserInfo, updateType: UserInfoUpdateType) = data.let {
        val isNoted = when (updateType) {
            UserInfoUpdateType.FINISHED -> false
            UserInfoUpdateType.NOTE -> true
            else -> data.isNoted
        }

        val isFinished = when (updateType) {
            UserInfoUpdateType.FINISHED -> true
            else -> data.isFinished
        }

        val isTopTen = when (updateType) {
            UserInfoUpdateType.FAVORITE -> true
            UserInfoUpdateType.UNFAVORITE -> false
            else -> data.isTopTen
        }

        val isSubscribed = when (updateType) {
            UserInfoUpdateType.SUBSCRIBE -> true
            UserInfoUpdateType.UNSUBSCRIBE -> false
            else -> data.isSubscribed
        }

        MediaUserInfo(isNoted, isFinished, data.isCanceled, isTopTen, isSubscribed)
    }

    private enum class UserInfoUpdateType {
        NOTE, FAVORITE, UNFAVORITE, FINISHED, SUBSCRIBE, UNSUBSCRIBE
    }
}
