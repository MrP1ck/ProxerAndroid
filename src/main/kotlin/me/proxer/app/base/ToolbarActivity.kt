package me.proxer.app.base

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.util.extension.resolveColor

/**
 * An Activity with a Toolbar and an up button, for the screens that are not part of the Compose navigation yet.
 *
 * @author Ruben Gees
 */
@Suppress("UnnecessaryAbstractClass")
abstract class ToolbarActivity : BaseActivity() {

    protected open val contentView
        get() = R.layout.activity_default

    /**
     * Whether the content is drawn behind the transparent status bar, e.g. for a header image.
     */
    protected open val drawsBehindStatusBar = false

    open val toolbar: Toolbar by bindView(R.id.toolbar)
    open val appbar: AppBarLayout by bindView(R.id.appbar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentView)
        setSupportActionBar(toolbar)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = when (drawsBehindStatusBar) {
            true -> Color.TRANSPARENT
            false -> resolveColor(R.attr.colorSurface)
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
