package me.proxer.app.settings

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.commitNow
import me.proxer.app.R
import me.proxer.app.base.ToolbarActivity
import me.proxer.app.util.extension.startActivity

/**
 * Hosts the [AboutFragment] until it is ported to Compose.
 */
class AboutActivity : ToolbarActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<AboutActivity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.section_info)

        if (savedInstanceState == null) {
            supportFragmentManager.commitNow {
                replace(R.id.container, AboutFragment.newInstance())
            }
        }
    }
}
