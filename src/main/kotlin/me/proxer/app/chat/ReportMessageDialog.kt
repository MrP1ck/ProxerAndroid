package me.proxer.app.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.util.extension.toast

/**
 * Lets the user report a message (or a conference) with a reason. Dismisses itself after the report was sent.
 */
@Composable
fun ReportMessageDialog(viewModel: ReportViewModel, id: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.observeAsState(false)
    val result by viewModel.data.observeAsState()
    val error by viewModel.error.observeAsState()

    var message by rememberSaveable { mutableStateOf("") }
    var isInvalid by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(result) {
        if (result != null) {
            viewModel.data.value = null
            onDismiss()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            viewModel.error.value = null
            context.toast(it.message)
        }
    }

    fun send() {
        if (message.isBlank()) isInvalid = true else viewModel.sendReport(id, message.trim())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_chat_report_title)) },
        text = {
            if (isLoading == true) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        message = it
                        isInvalid = false
                    },
                    label = { Text(stringResource(R.string.dialog_chat_report_message_hint)) },
                    isError = isInvalid,
                    supportingText = if (isInvalid) {
                        { Text(stringResource(R.string.dialog_chat_error_message)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { send() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = ::send, enabled = isLoading != true) {
                Text(stringResource(R.string.dialog_chat_report_positive))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
