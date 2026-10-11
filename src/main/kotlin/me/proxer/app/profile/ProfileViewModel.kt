package me.proxer.app.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.profile.ProfileViewModel.UserInfoWrapper
import me.proxer.app.util.extension.await
import me.proxer.library.entity.user.UserInfo

/**
 * @author Ruben Gees
 */
class ProfileViewModel(
    private val userId: String?,
    private val username: String?
) : BaseViewModel<UserInfoWrapper>() {

    override suspend fun loadData(): UserInfoWrapper {
        validate()

        val userInfo = api.user.info(userId, username).await()
        val watchedEpisodes = when (storageHelper.user?.matches(userId, username) == true) {
            true -> api.ucp.watchedEpisodes().await()
            false -> null
        }

        return UserInfoWrapper(userInfo, watchedEpisodes)
    }

    init {
        viewModelScope.launch { storageHelper.isLoggedInChanges.collect { reload() } }
    }

    data class UserInfoWrapper(val info: UserInfo, val watchedEpisodes: Int?)
}
