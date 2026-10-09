package me.proxer.app.anime

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.LabeledCheckbox
import me.proxer.app.ui.components.ProxerDialogContent
import me.proxer.app.util.extension.getSafeString

/**
 * Warns that a stream is about to be played over a cellular connection.
 *
 * @author Ruben Gees
 */
class NoWifiDialog : ComposeDialog() {

    companion object {
        const val STREAM_ID_RESULT = "stream_id"

        private const val STREAM_ID_ARGUMENT = "stream_id"

        fun show(activity: AppCompatActivity, streamId: String) = NoWifiDialog()
            .apply { arguments = bundleOf(STREAM_ID_ARGUMENT to streamId) }
            .show(activity.supportFragmentManager, "no_wifi_dialog")
    }

    private val streamId: String
        get() = requireArguments().getSafeString(STREAM_ID_ARGUMENT)

    @Composable
    override fun DialogContent() {
        var remember by rememberSaveable { mutableStateOf(false) }

        ProxerDialogContent(
            confirmButton = {
                DialogButton(R.string.dialog_no_wifi_positive, onClick = {
                    if (remember) {
                        preferenceHelper.shouldCheckCellular = false
                    }

                    setFragmentResult(STREAM_ID_RESULT, bundleOf(STREAM_ID_RESULT to streamId))
                    dismiss()
                })
            },
            dismissButton = { DialogButton(R.string.cancel, onClick = ::dismiss) }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.dialog_no_wifi_content).trim())

                LabeledCheckbox(remember, { remember = it }, stringResource(R.string.dialog_no_wifi_remember))
            }
        }
    }
}
