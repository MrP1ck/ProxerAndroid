package me.proxer.app.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import me.proxer.app.R
import me.proxer.app.ui.theme.ProxerAppTheme
import me.proxer.app.util.CustomTabsConnection
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import okhttp3.HttpUrl
import org.koin.android.ext.android.inject
import kotlin.properties.Delegates

/**
 * A dialog rendered with Compose. Unlike a dialog composable, it survives configuration changes and can be shown from
 * any activity, e.g. the login dialog when an error requires a login.
 *
 * The [DialogContent] draws the dialog surface itself, usually with [me.proxer.app.ui.components.ProxerDialogContent].
 */
abstract class ComposeDialog : AppCompatDialogFragment(), CustomTabsAware {

    protected val storageHelper by inject<StorageHelper>()
    protected val preferenceHelper by inject<PreferenceHelper>()

    protected var customTabsConnection by Delegates.notNull<CustomTabsConnection>()
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_App_ComposeDialog)

        customTabsConnection = CustomTabsConnection(requireContext()).also { lifecycle.addObserver(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ProxerAppTheme { DialogContent() } }
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @Composable
    protected abstract fun DialogContent()

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsConnection.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean, skipCheck: Boolean) {
        customTabsConnection.fallbackHandleLink(requireActivity(), url, forceBrowser, skipCheck)
    }
}
