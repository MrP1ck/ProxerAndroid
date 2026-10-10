package me.proxer.app.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import me.proxer.app.auth.LocalUser
import me.proxer.app.util.data.StorageHelper
import org.koin.compose.koinInject

/**
 * The logged in user, or null for guests. Updates on login and logout.
 */
@Composable
fun rememberCurrentUser(): State<LocalUser?> {
    val storageHelper = koinInject<StorageHelper>()

    return produceState(storageHelper.user, storageHelper) {
        val disposable = storageHelper.isLoggedInObservable.subscribe { value = storageHelper.user }

        awaitDispose { disposable.dispose() }
    }
}
