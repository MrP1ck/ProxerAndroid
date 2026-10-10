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
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.safeInject
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * A dialog rendered with Compose. Unlike a dialog composable, it survives configuration changes and can be shown from
 * any activity, e.g. the login dialog when an error requires a login.
 *
 * The [DialogContent] draws the dialog surface itself, usually with [me.proxer.app.ui.components.ProxerDialogContent].
 */
abstract class ComposeDialog : AppCompatDialogFragment(), CustomTabsAware {

    protected val storageHelper by safeInject<StorageHelper>()
    protected val preferenceHelper by safeInject<PreferenceHelper>()

    protected var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.ThemeOverlay_App_ComposeDialog)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)
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
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean, skipCheck: Boolean) {
        customTabsHelper.fallbackHandleLink(requireActivity(), url, forceBrowser, skipCheck)
    }
}
