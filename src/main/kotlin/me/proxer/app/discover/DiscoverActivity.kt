package me.proxer.app.discover

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toPrefixedHttpUrl
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl

private val DISCORD_URL = "https://discord.gg/XwrEDmA".toPrefixedHttpUrl()
private val STATUS_URL = "https://status.proxer.me/".toPrefixedHttpUrl()

/**
 * The areas of the website, which are not part of the main navigation: the directories, which can be browsed in the
 * app, and links to the parts which are only available on the website.
 */
class DiscoverActivity : ComposeActivity() {

    companion object {
        fun navigateTo(context: Activity) {
            context.startActivity<DiscoverActivity>()
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current

        DiscoverScreen(
            onUp = navigator::navigateUp,
            onDirectoryClick = navigator::openDirectory,
            onWebsiteClick = { navigator.showPage(it, skipCheck = true) }
        )
    }
}

@Composable
fun DiscoverScreen(onUp: () -> Unit, onDirectoryClick: (DirectoryKind) -> Unit, onWebsiteClick: (HttpUrl) -> Unit) {
    ProxerScaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.section_discover)) }, navigationIcon = { UpButton(onUp) })
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "directories_header") { SectionHeader(stringResource(R.string.discover_directories)) }

            DirectoryKind.entries.forEach { kind ->
                item(key = kind.name) {
                    DiscoverItem(kind.icon, kind.title) { onDirectoryClick(kind) }
                }
            }

            item(key = "website_header") { SectionHeader(stringResource(R.string.discover_website)) }

            listOf(
                Triple(R.string.discover_touzai, R.drawable.ic_symbol_newspaper, webUrl("touzai")),
                Triple(R.string.discover_proxcast, R.drawable.ic_symbol_smart_display, webUrl("proxcast")),
                Triple(R.string.discover_reviews, R.drawable.ic_symbol_star, webUrl("review")),
                Triple(R.string.discover_discord, R.drawable.ic_symbol_forum, DISCORD_URL),
                Triple(R.string.discover_status, R.drawable.ic_symbol_cloud_off, STATUS_URL)
            ).forEach { (title, icon, url) ->
                item(key = url.toString()) {
                    DiscoverItem(icon, title) { onWebsiteClick(url) }
                }
            }
        }
    }
}

private fun webUrl(path: String) = ProxerUrls.webBase.newBuilder().addPathSegment(path).build()

@Composable
private fun DiscoverItem(@DrawableRes icon: Int, @StringRes title: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
