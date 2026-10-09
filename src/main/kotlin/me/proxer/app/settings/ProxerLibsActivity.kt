package me.proxer.app.settings

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.commitNow
import com.mikepenz.aboutlibraries.LibsBuilder
import me.proxer.app.R
import me.proxer.app.base.DrawerActivity
import me.proxer.app.util.extension.startActivity

class ProxerLibsActivity : DrawerActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ProxerLibsActivity>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        title = getString(R.string.about_licenses_activity_title)

        if (savedInstanceState == null) {
            val fragment = LibsBuilder()
                .withShowLoadingProgress(false)
                .withAboutVersionShown(false)
                .withAboutIconShown(false)
                .withVersionShown(false)
                .supportFragment()

            supportFragmentManager.commitNow {
                replace(R.id.container, fragment)
            }
        }
    }
}
