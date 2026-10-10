package me.proxer.app.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import me.proxer.app.BuildConfig
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.settings.status.ServerStatusActivity
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toPrefixedHttpUrl
import me.proxer.app.util.extension.toast
import okhttp3.HttpUrl

/**
 * Information about the app: its version, licenses, social media and support.
 */
class AboutActivity : ComposeActivity() {

    companion object {
        private val teamLink = "https://proxer.me/team?device=default".toPrefixedHttpUrl()
        private val facebookLink = "https://facebook.com/Anime.Proxer.Me".toPrefixedHttpUrl()
        private val twitterLink = "https://twitter.com/proxerme".toPrefixedHttpUrl()
        private val youtubeLink = "https://youtube.com/channel/UC7h-fT9Y9XFxuZ5GZpbcrtA".toPrefixedHttpUrl()
        private val discordLink = "https://discord.gg/XwrEDmA".toPrefixedHttpUrl()
        private val repositoryLink = "https://github.com/proxer/ProxerAndroid".toPrefixedHttpUrl()

        private const val SUPPORT_MAIL = "appsupport@proxer.de"
        private const val DEVELOPER_GITHUB_NAME = "rubengees"

        fun navigateTo(context: Activity) = context.startActivity<AboutActivity>()
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current

        ProxerScaffold(title = stringResource(R.string.section_info), onNavigateUp = navigator::navigateUp) { padding ->
            LazyColumn(contentPadding = padding) {
                item { AppHeader() }

                item {
                    AboutItem(R.drawable.ic_symbol_sell, R.string.about_version_title, BuildConfig.VERSION_NAME) {
                        getSystemService<ClipboardManager>()?.setPrimaryClip(
                            ClipData.newPlainText(getString(R.string.clipboard_title), BuildConfig.VERSION_NAME)
                        )

                        toast(R.string.clipboard_status)
                    }

                    AboutItem(
                        R.drawable.ic_symbol_description,
                        R.string.about_licenses_title,
                        stringResource(R.string.about_licenses_description)
                    ) {
                        ProxerLibsActivity.navigateTo(this@AboutActivity)
                    }

                    AboutItem(
                        R.drawable.ic_symbol_code,
                        R.string.about_source_code,
                        stringResource(R.string.about_source_code_description)
                    ) {
                        openLink(repositoryLink)
                    }

                    AboutItem(
                        R.drawable.ic_symbol_dns,
                        R.string.about_server_status,
                        stringResource(R.string.about_server_status_description)
                    ) {
                        ServerStatusActivity.navigateTo(this@AboutActivity)
                    }
                }

                item {
                    PreferenceCategory(stringResource(R.string.about_social_media_title))

                    AboutItem(
                        R.drawable.ic_symbol_public,
                        R.string.about_facebook_title,
                        stringResource(R.string.about_facebook_description)
                    ) {
                        openLink(facebookLink)
                    }

                    AboutItem(
                        R.drawable.ic_symbol_public,
                        R.string.about_twitter_title,
                        stringResource(R.string.about_twitter_description)
                    ) {
                        openLink(twitterLink)
                    }

                    AboutItem(
                        R.drawable.ic_symbol_smart_display,
                        R.string.about_youtube_title,
                        stringResource(R.string.about_youtube_description)
                    ) {
                        openLink(youtubeLink)
                    }

                    AboutItem(
                        R.drawable.ic_symbol_forum,
                        R.string.about_discord_title,
                        stringResource(R.string.about_discord_description)
                    ) {
                        openLink(discordLink)
                    }
                }

                item {
                    PreferenceCategory(stringResource(R.string.about_support_title))

                    Preference(
                        title = stringResource(R.string.about_support_title),
                        summary = stringResource(R.string.about_support_info).trim(),
                        icon = R.drawable.ic_symbol_support_agent,
                        onClick = { openLink(teamLink) }
                    )

                    AboutItem(
                        R.drawable.ic_symbol_mail,
                        R.string.about_support_mail_title,
                        stringResource(R.string.about_support_mail_description)
                    ) {
                        sendSupportMail()
                    }
                }

                item {
                    PreferenceCategory(stringResource(R.string.about_developer_title))

                    AboutItem(R.drawable.ic_symbol_code, R.string.about_developer_github_title, DEVELOPER_GITHUB_NAME) {
                        openLink("https://github.com/$DEVELOPER_GITHUB_NAME".toPrefixedHttpUrl())
                    }
                }
            }
        }
    }

    @Composable
    private fun AppHeader() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    painter = painterResource(R.drawable.ic_proxer),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(56.dp)
                )
            }

            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
        }
    }

    @Composable
    private fun AboutItem(@DrawableRes icon: Int, title: Int, summary: String, onClick: () -> Unit) {
        Preference(title = stringResource(title), summary = summary, icon = icon, onClick = onClick)
    }

    private fun openLink(url: HttpUrl) = showPage(url, forceBrowser = false, skipCheck = true)

    private fun sendSupportMail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:".toUri()
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_MAIL))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_support_mail_subject))
        }

        try {
            startActivity(intent)
        } catch (ignored: ActivityNotFoundException) {
            toast(R.string.about_error_mail_no_activity)
        }
    }
}
