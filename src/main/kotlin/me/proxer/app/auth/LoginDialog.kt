package me.proxer.app.auth

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.base.ComposeDialog
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.ProxerDialogContent
import me.proxer.app.util.extension.toast
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Logs the user in with username, password and, if two factor authentication is enabled, the secret key.
 *
 * @author Ruben Gees
 */
class LoginDialog : ComposeDialog() {

    companion object {
        private val websiteRegex = Regex("Proxer \\b(.+?)\\b")

        fun show(activity: AppCompatActivity) = LoginDialog().show(activity.supportFragmentManager, "login_dialog")
    }

    private val viewModel by viewModel<LoginViewModel>()

    @Composable
    override fun DialogContent() {
        val isLoading by viewModel.isLoading.observeAsState()
        val isTwoFactorAuthenticationEnabled by viewModel.isTwoFactorAuthenticationEnabled.observeAsState()
        val success by viewModel.success.observeAsState()
        val error by viewModel.error.observeAsState()

        var username by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var secretKey by rememberSaveable { mutableStateOf("") }
        var usernameError by rememberSaveable { mutableStateOf<Int?>(null) }
        var passwordError by rememberSaveable { mutableStateOf<Int?>(null) }
        var isPasswordVisible by rememberSaveable { mutableStateOf(false) }

        val usernameFocus = remember { FocusRequester() }
        val hasSecretKey = isTwoFactorAuthenticationEnabled == true

        fun login() {
            usernameError = R.string.dialog_login_error_username.takeIf { username.isBlank() }
            passwordError = R.string.dialog_login_error_password.takeIf { usernameError == null && password.isBlank() }

            if (usernameError == null && passwordError == null) {
                viewModel.login(username.trim(), password.trim(), secretKey.trim())
            }
        }

        LaunchedEffect(Unit) { setLikelyUrl(ProxerUrls.registerWeb()) }
        LaunchedEffect(success) { if (success != null) dismiss() }

        LaunchedEffect(error) {
            error?.let {
                viewModel.error.value = null

                requireContext().toast(it.message)
            }
        }

        LaunchedEffect(isLoading) { if (isLoading != true) runCatching { usernameFocus.requestFocus() } }

        ProxerDialogContent(
            title = stringResource(R.string.dialog_login_title),
            confirmButton = {
                DialogButton(
                    R.string.dialog_login_positive,
                    onClick = ::login,
                    enabled = isLoading != true
                )
            },
            dismissButton = { DialogButton(R.string.cancel, onClick = ::dismiss) }
        ) {
            if (isLoading == true) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = null
                        },
                        label = { Text(stringResource(R.string.dialog_login_username_hint)) },
                        isError = usernameError != null,
                        supportingText = usernameError?.let { { Text(stringResource(it)) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(usernameFocus)
                            .semantics { contentType = ContentType.Username }
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = null
                        },
                        label = { Text(stringResource(R.string.dialog_login_password_hint)) },
                        isError = passwordError != null,
                        supportingText = passwordError?.let { { Text(stringResource(it)) } },
                        singleLine = true,
                        visualTransformation = when (isPasswordVisible) {
                            true -> VisualTransformation.None
                            false -> PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(
                                        when (isPasswordVisible) {
                                            true -> R.drawable.ic_symbol_visibility_off
                                            false -> R.drawable.ic_symbol_visibility
                                        }
                                    ),
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (hasSecretKey) ImeAction.Next else ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { login() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Password }
                    )

                    if (hasSecretKey) {
                        OutlinedTextField(
                            value = secretKey,
                            onValueChange = { secretKey = it },
                            label = { Text(stringResource(R.string.dialog_login_secret_hint)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { login() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentType = ContentType.SmsOtpCode }
                        )
                    }

                    RegistrationInfo(Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    @Composable
    private fun RegistrationInfo(modifier: Modifier = Modifier) {
        val text = stringResource(R.string.dialog_login_registration).trim()
        val linkStyle = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)

        val annotatedText = remember(text, linkStyle) {
            buildAnnotatedString {
                append(text)

                websiteRegex.find(text)?.groups?.get(1)?.range?.let { range ->
                    addLink(
                        LinkAnnotation.Clickable("register", TextLinkStyles(linkStyle)) {
                            showPage(ProxerUrls.registerWeb(), forceBrowser = false, skipCheck = true)
                        },
                        range.first,
                        range.last + 1
                    )
                }
            }
        }

        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_symbol_info), contentDescription = null, modifier = Modifier.size(20.dp))

            Spacer(Modifier.width(12.dp))

            Text(annotatedText, style = MaterialTheme.typography.bodySmall)
        }
    }
}
