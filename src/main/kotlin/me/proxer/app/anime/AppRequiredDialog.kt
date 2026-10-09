package me.proxer.app.anime

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.ProxerDialogContent
import me.proxer.app.util.extension.getSafeString

/**
 * Tells the user that an app is required to play a stream and offers to install it.
 *
 * @author Ruben Gees
 */
class AppRequiredDialog : ComposeDialog() {

    companion object {
        private const val NAME_ARGUMENT = "name"
        private const val PACKAGE_NAME_ARGUMENT = "package_name"

        fun show(activity: AppCompatActivity, name: String, packageName: String) = AppRequiredDialog().apply {
            arguments = bundleOf(NAME_ARGUMENT to name, PACKAGE_NAME_ARGUMENT to packageName)
        }.show(activity.supportFragmentManager, "app_required_dialog")
    }

    private val name get() = requireArguments().getSafeString(NAME_ARGUMENT)
    private val packageName get() = requireArguments().getSafeString(PACKAGE_NAME_ARGUMENT)

    @Composable
    override fun DialogContent() {
        ProxerDialogContent(
            title = stringResource(R.string.dialog_app_required_title, name),
            confirmButton = {
                DialogButton(R.string.dialog_app_required_positive, onClick = {
                    openStore()
                    dismiss()
                })
            },
            dismissButton = { DialogButton(R.string.cancel, onClick = ::dismiss) }
        ) {
            Text(stringResource(R.string.dialog_app_required_content, name))
        }
    }

    private fun openStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()))
        } catch (error: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()))
        }
    }
}
