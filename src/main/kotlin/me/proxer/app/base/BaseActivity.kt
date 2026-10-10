package me.proxer.app.base

import android.os.Bundle
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.rubengees.rxbus.RxBus
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDisposable
import me.proxer.app.R
import me.proxer.app.settings.theme.ThemeContainer
import me.proxer.app.settings.theme.applyThemeContainer
import me.proxer.app.util.compat.TaskDescriptionCompat
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.fallbackHandleLink
import me.proxer.app.util.extension.safeInject
import me.zhanghai.android.customtabshelper.CustomTabsHelperFragment
import okhttp3.HttpUrl
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class BaseActivity : AppCompatActivity(), CustomTabsAware {

    private companion object {
        private const val STATE = "activity_state"
    }

    protected open val theme
        @StyleRes get() = preferenceHelper.themeContainer.theme.main

    /**
     * An optional theme overlay applied on top of the app theme, e.g. [R.style.ThemeOverlay_App_OnImage].
     */
    protected open val themeOverlay: Int?
        @StyleRes get() = null

    protected val bus by safeInject<RxBus>()
    protected val storageHelper by safeInject<StorageHelper>()
    protected val preferenceHelper by safeInject<PreferenceHelper>()

    private var customTabsHelper by Delegates.notNull<CustomTabsHelperFragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        // This needs to be called before super.onCreate(), because otherwise Fragments might not see the
        // restored state in time.
        savedInstanceState?.getBundle(STATE)?.let { state ->
            intent.putExtras(state)
        }

        applyThemeContainer(preferenceHelper.themeContainer, theme)
        themeOverlay?.let { getTheme().applyStyle(it, true) }

        TaskDescriptionCompat.setTaskDescription(this, preferenceHelper.themeContainer.theme.primaryColor(this))

        super.onCreate(savedInstanceState)

        customTabsHelper = CustomTabsHelperFragment.attachTo(this)

        preferenceHelper.themeObservable
            .autoDisposable(this.scope())
            .subscribe { onThemeChanged(it) }
    }

    /**
     * Called when the user changed the theme. Recreates the Activity by default, so the Views are inflated with the new
     * colors. Changes of the light/dark variant also recreate the Activity through AppCompat.
     */
    protected open fun onThemeChanged(themeContainer: ThemeContainer) {
        ActivityCompat.recreate(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        intent.extras?.let { state ->
            outState.putBundle(STATE, state)
        }
    }

    override fun setLikelyUrl(url: HttpUrl): Boolean {
        return customTabsHelper.mayLaunchUrl(url.androidUri(), bundleOf(), emptyList())
    }

    override fun showPage(url: HttpUrl, forceBrowser: Boolean, skipCheck: Boolean) {
        customTabsHelper.fallbackHandleLink(this, url, forceBrowser, skipCheck)
    }
}
