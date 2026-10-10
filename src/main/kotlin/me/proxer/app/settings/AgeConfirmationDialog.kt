package me.proxer.app.settings

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.ProxerDialogContent

/**
 * Asks the user to confirm being of age to show age restricted content.
 *
 * @author Ruben Gees
 */
class AgeConfirmationDialog : ComposeDialog() {

    companion object {
        fun show(activity: AppCompatActivity) = AgeConfirmationDialog()
            .show(activity.supportFragmentManager, "age_confirmation_dialog")
    }

    @Composable
    override fun DialogContent() {
        ProxerDialogContent(
            icon = { Icon(painterResource(R.drawable.ic_symbol_no_adult_content), contentDescription = null) },
            confirmButton = {
                DialogButton(R.string.dialog_age_confirmation_positive, onClick = {
                    preferenceHelper.isAgeRestrictedMediaAllowed = true

                    dismiss()
                })
            },
            dismissButton = { DialogButton(R.string.cancel, onClick = ::dismiss) }
        ) {
            Text(stringResource(R.string.dialog_age_confirmation_content).trim())
        }
    }
}
