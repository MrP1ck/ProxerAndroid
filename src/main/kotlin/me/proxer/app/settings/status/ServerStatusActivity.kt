package me.proxer.app.settings.status

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.startActivity
import org.koin.androidx.compose.koinViewModel

/**
 * The status of the servers of Proxer.
 *
 * @author Ruben Gees
 */
class ServerStatusActivity : ComposeActivity() {

    companion object {
        fun navigateTo(context: Activity) = context.startActivity<ServerStatusActivity>()
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val viewModel = koinViewModel<ServerStatusViewModel>()
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)
        var isRefreshRequested by rememberSaveable { mutableStateOf(false) }

        if (!state.isLoading) isRefreshRequested = false

        ProxerScaffold(
            title = stringResource(R.string.section_server_status),
            onNavigateUp = navigator::navigateUp
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = isRefreshRequested && state.isLoading,
                onRefresh = {
                    isRefreshRequested = true

                    viewModel.refresh()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                ContentStateHost(state, onErrorAction) { servers ->
                    ServerStatusContent(servers, PaddingValues(bottom = padding.calculateBottomPadding()))
                }
            }
        }
    }
}

@Composable
fun ServerStatusContent(servers: List<ServerStatus>, contentPadding: PaddingValues = PaddingValues()) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = contentPadding + PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OverallStatus(servers.all { it.online })
        }

        items(servers, key = { "${it.type}_${it.number}" }) { server ->
            ServerCard(server)
        }
    }
}

@Composable
private fun OverallStatus(isOnline: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (isOnline) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (isOnline) R.drawable.ic_symbol_cloud_done else R.drawable.ic_symbol_cloud_off),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = stringResource(
                    when (isOnline) {
                        true -> R.string.fragment_server_status_overall_online
                        false -> R.string.fragment_server_status_overall_offline
                    }
                ).trim(),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun ServerCard(server: ServerStatus) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        when (server.type) {
                            ServerType.MAIN -> R.drawable.ic_symbol_dns
                            ServerType.MANGA -> R.drawable.ic_symbol_menu_book
                            ServerType.STREAM -> R.drawable.ic_symbol_live_tv
                        }
                    ),
                    contentDescription = null
                )

                Spacer(Modifier.weight(1f))

                Icon(
                    painter = painterResource(
                        if (server.online) R.drawable.ic_symbol_check_circle_filled else R.drawable.ic_symbol_error
                    ),
                    contentDescription = null,
                    tint = if (server.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }

            Text(server.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
