package me.proxer.app.ui.view

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import me.proxer.app.BuildConfig.APPLICATION_ID
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.ProxerDialogContent

/**
 * Asks the user to rate the app in the Play Store.
 *
 * @author Ruben Gees
 */
class RatingDialog : ComposeDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = RatingDialog()
            .show(activity.supportFragmentManager, "rating_dialog")
    }

    @Composable
    override fun DialogContent() {
        ProxerDialogContent(
            title = stringResource(R.string.dialog_rating_title),
            icon = { Icon(painterResource(R.drawable.ic_symbol_star), contentDescription = null) },
            confirmButton = {
                DialogButton(R.string.dialog_rating_positive, onClick = {
                    preferenceHelper.hasRated = true

                    openStore()
                    dismiss()
                })
            },
            dismissButton = {
                DialogButton(R.string.dialog_rating_negative, onClick = {
                    preferenceHelper.hasRated = true

                    dismiss()
                })
            },
            neutralButton = { DialogButton(R.string.dialog_rating_neutral, onClick = ::dismiss) }
        ) {
            Text(stringResource(R.string.dialog_rating_content).trim())
        }
    }

    private fun openStore() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$APPLICATION_ID".toUri()))
        } catch (error: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$APPLICATION_ID".toUri()))
        }
    }
}
