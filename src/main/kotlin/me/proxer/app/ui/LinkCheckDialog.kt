package me.proxer.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.LabeledCheckbox
import me.proxer.app.ui.components.ProxerDialogContent
import me.proxer.app.util.extension.getSafeString
import me.proxer.app.util.extension.openHttpPage
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Warns before opening an external link and shows whether the link is known to be harmful.
 */
class LinkCheckDialog : ComposeDialog() {

    companion object {
        private const val LINK_ARGUMENT = "link"

        fun show(activity: FragmentActivity, link: HttpUrl) = LinkCheckDialog()
            .apply { arguments = bundleOf(LINK_ARGUMENT to link.toString()) }
            .show(activity.supportFragmentManager, "link_check_dialog")
    }

    private val viewModel by viewModel<LinkCheckViewModel>()

    private val link: HttpUrl
        get() = requireArguments().getSafeString(LINK_ARGUMENT).toHttpUrl()

    @Composable
    override fun DialogContent() {
        val isSecure by viewModel.data.observeAsState()
        val isLoading by viewModel.isLoading.observeAsState()
        var remember by rememberSaveable { mutableStateOf(false) }
        val message = stringResource(R.string.dialog_link_check_message, link.toString()).trim()

        LaunchedEffect(
            Unit
        ) { if (viewModel.data.value == null && viewModel.isLoading.value != true) viewModel.check(link) }

        ProxerDialogContent(
            confirmButton = {
                DialogButton(R.string.dialog_link_check_positive, onClick = {
                    if (remember) {
                        preferenceHelper.shouldCheckLinks = false
                    }

                    customTabsHelper.openHttpPage(requireActivity(), link)
                    dismiss()
                })
            },
            dismissButton = { DialogButton(R.string.cancel, onClick = ::dismiss) }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(remember(message) { AnnotatedString.fromHtml(message) })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isLoading == true -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
                        isSecure == true -> Icon(
                            painterResource(R.drawable.ic_symbol_verified_user),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        isSecure == false -> Icon(
                            painterResource(R.drawable.ic_symbol_gpp_bad),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = stringResource(
                            when {
                                isLoading == true -> R.string.dialog_link_check_progress
                                isSecure == false -> R.string.dialog_link_check_not_secure
                                else -> R.string.dialog_link_check_secure
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = when (isSecure == false) {
                            true -> MaterialTheme.colorScheme.error
                            false -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                LabeledCheckbox(remember, { remember = it }, stringResource(R.string.dialog_no_wifi_remember))
            }
        }
    }
}
