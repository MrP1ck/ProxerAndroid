package me.proxer.app.ui.components

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.rubengees.rxbus.RxBus
import me.proxer.app.auth.LoginDialog
import me.proxer.app.base.BaseActivity
import me.proxer.app.base.BaseViewModel
import me.proxer.app.base.CaptchaSolvedEvent
import me.proxer.app.base.PagedViewModel
import me.proxer.app.settings.AgeConfirmationDialog
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.Utils
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import org.koin.compose.koinInject

/**
 * The state of a screen backed by a [BaseViewModel]: its [data], a blocking [error] and whether it [isLoading].
 * Paged screens additionally have a [refreshError], which occurs when refreshing fails while data is shown.
 */
@Immutable
data class ContentState<out T>(
    val data: T? = null,
    val error: ErrorAction? = null,
    val isLoading: Boolean = false,
    val refreshError: ErrorAction? = null
) {

    /** True while loading and nothing is shown yet, e.g. on first load. */
    val isInitialLoading get() = isLoading && data == null && error == null
}

/**
 * Observes this ViewModel as a [ContentState] and loads the data on first composition, like the Fragments did.
 */
@Composable
fun <T> BaseViewModel<T>.collectContentState(): ContentState<T> {
    val data by data.observeAsState()
    val error by error.observeAsState()
    val isLoading by isLoading.observeAsState(false)
    val refreshError = (this as? PagedViewModel<*>)?.refreshError?.observeAsState()?.value

    LoadOnFirstComposition(this)

    return ContentState(data, error, isLoading == true, refreshError)
}

@Composable
private fun LoadOnFirstComposition(viewModel: BaseViewModel<*>) {
    var hasRequestedLoad by rememberSaveable { mutableStateOf(false) }

    if (!hasRequestedLoad) {
        hasRequestedLoad = true

        if (viewModel.isLoading.value != true && viewModel.data.value == null && viewModel.error.value == null) {
            viewModel.load()
        }
    }
}

/**
 * Returns a handler for the button of an [ErrorAction], e.g. to show the login dialog or to open the captcha.
 * Errors without special action are retried with [onRetry].
 *
 * After the user returns from solving a captcha, a [CaptchaSolvedEvent] is posted, which makes all ViewModels with a
 * captcha error reload.
 */
@Composable
fun rememberErrorActionHandler(onRetry: () -> Unit): (ErrorAction) -> Unit {
    val activity = LocalActivity.current as? BaseActivity
    val bus = koinInject<RxBus>()
    var isSolvingCaptcha by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(isSolvingCaptcha) {
        if (isSolvingCaptcha) {
            isSolvingCaptcha = false

            bus.post(CaptchaSolvedEvent())
        }

        onPauseOrDispose { }
    }

    return remember(activity, onRetry) {
        { action ->
            if (activity == null) {
                onRetry()
            } else {
                when (action.buttonAction) {
                    ButtonAction.CAPTCHA -> {
                        isSolvingCaptcha = true

                        activity.showPage(ProxerUrls.captchaWeb(Utils.getIpAddress(), Device.MOBILE), skipCheck = true)
                    }
                    ButtonAction.NETWORK_SETTINGS -> activity.startActivity(
                        when {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> Intent(
                                Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                            )
                            else -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
                        }
                    )
                    ButtonAction.LOGIN -> LoginDialog.show(activity)
                    ButtonAction.AGE_CONFIRMATION -> AgeConfirmationDialog.show(activity)
                    ButtonAction.OPEN_LINK -> when (val link = action.data[ErrorUtils.LINK_DATA_KEY]) {
                        is HttpUrl -> activity.showPage(link, skipCheck = true)
                        else -> onRetry()
                    }
                    ButtonAction.BOOKMARK, null -> onRetry()
                }
            }
        }
    }
}
