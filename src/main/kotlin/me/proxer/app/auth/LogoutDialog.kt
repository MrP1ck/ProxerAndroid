package me.proxer.app.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.ProxerDialogContent
import me.proxer.app.util.extension.toast
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Asks the user to confirm logging out and logs out.
 *
 * @author Ruben Gees
 */
class LogoutDialog : ComposeDialog() {

    companion object {
        fun show(activity: FragmentActivity) = LogoutDialog().show(activity.supportFragmentManager, "logout_dialog")
    }

    private val viewModel by viewModel<LogoutViewModel>()

    @Composable
    override fun DialogContent() {
        val isLoading by viewModel.isLoading.observeAsState()
        val success by viewModel.success.observeAsState()
        val error by viewModel.error.observeAsState()

        LaunchedEffect(success) { if (success != null) dismiss() }

        LaunchedEffect(error) {
            error?.let {
                viewModel.error.value = null

                requireContext().toast(it.message)
            }
        }

        ProxerDialogContent(
            icon = { Icon(painterResource(R.drawable.ic_symbol_logout), contentDescription = null) },
            confirmButton = {
                DialogButton(R.string.dialog_logout_positive, onClick = viewModel::logout, enabled = isLoading != true)
            },
            dismissButton = { DialogButton(R.string.cancel, onClick = ::dismiss) }
        ) {
            if (isLoading == true) {
                Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Text(stringResource(R.string.dialog_logout_content).trim())
            }
        }
    }
}
