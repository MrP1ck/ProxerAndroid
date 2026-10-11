package me.proxer.app.chat.pub.room

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.ViewModelDependencies
import me.proxer.app.util.extension.await
import me.proxer.library.entity.chat.ChatRoom

/**
 * @author Ruben Gees
 */
class ChatRoomViewModel(dependencies: ViewModelDependencies) : BaseViewModel<List<ChatRoom>>(dependencies) {

    override suspend fun loadData(): List<ChatRoom> {
        validate()

        val rooms = coroutineScope {
            val publicRooms = async { api.chat.publicRooms().await() }
            val userRooms = if (storageHelper.isLoggedIn) async { api.chat.userRooms().await() } else null

            publicRooms.await() + (userRooms?.await() ?: emptyList())
        }

        return rooms
            .asSequence()
            .distinctBy { room -> room.id }
            .sortedBy { room -> room.id }
            .toList()
    }

    init {
        viewModelScope.launch { storageHelper.isLoggedInChanges.collect { reload() } }
    }
}
