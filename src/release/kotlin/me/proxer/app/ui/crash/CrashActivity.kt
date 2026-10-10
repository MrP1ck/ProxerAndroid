package me.proxer.app.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.util.extension.toast

/**
 * Shown after the app crashed in release builds. Offers to copy the error report or to restart the app.
 *
 * @author Ruben Gees
 */
class CrashActivity : ComposeActivity() {

    private companion object {
        private const val DEVELOPER_PROXER_NAME = "RubyGee"
    }

    private val config: CaocConfig
        get() = try {
            CustomActivityOnCrash.getConfigFromIntent(intent)
        } catch (ignored: Exception) { // Workaround for a bug in Caoc.
            CaocConfig()
        } ?: CaocConfig()

    private val errorDetails: String
        get() {
            val androidVersion = "Android version: ${Build.VERSION.RELEASE}\n"

            return androidVersion + CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        }

    @Composable
    override fun Content() {
        var isReportVisible by rememberSaveable { mutableStateOf(false) }

        ProxerScaffold(title = stringResource(R.string.section_crash), scrollBehavior = null) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.explosion),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.heightIn(max = 240.dp)
                )

                CrashText()

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { isReportVisible = true }) {
                        Text(stringResource(R.string.activity_crash_report))
                    }

                    Button(onClick = { CustomActivityOnCrash.restartApplication(this@CrashActivity, config) }) {
                        Text(stringResource(R.string.activity_crash_restart))
                    }
                }
            }
        }

        if (isReportVisible) {
            ReportDialog(onDismiss = { isReportVisible = false })
        }
    }

    @Composable
    private fun CrashText() {
        val text = stringResource(R.string.activity_crash_text).trim()
        val mention = "@$DEVELOPER_PROXER_NAME"
        val linkStyle = TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary))

        val annotatedText = remember(text, linkStyle) {
            buildAnnotatedString {
                append(text)

                val start = text.indexOf(mention)

                if (start >= 0) {
                    addLink(
                        LinkAnnotation.Clickable("developer", linkStyle) {
                            CustomActivityOnCrash.restartApplicationWithIntent(
                                this@CrashActivity,
                                CreateConferenceActivity.getIntent(
                                    this@CrashActivity,
                                    false,
                                    Participant(DEVELOPER_PROXER_NAME)
                                ),
                                config
                            )
                        },
                        start,
                        start + mention.length
                    )
                }
            }
        }

        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    @Composable
    private fun ReportDialog(onDismiss: () -> Unit) {
        val details = remember { errorDetails }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_crash_title)) },
            text = {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { DialogButton(R.string.dialog_crash_negative, onClick = onDismiss) },
            dismissButton = {
                DialogButton(R.string.dialog_crash_neutral, onClick = {
                    getSystemService<ClipboardManager>()?.setPrimaryClip(
                        ClipData.newPlainText(getString(R.string.clipboard_crash_title), details)
                    )

                    toast(R.string.clipboard_status)
                })
            }
        )
    }
}
