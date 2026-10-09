package me.proxer.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import me.proxer.app.R

/**
 * The scaffold of a screen: an edge-to-edge [TopAppBar] which collapses on scroll, a snackbar host (exposed through
 * [LocalSnackbarHostState]) and an optional floating action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxerScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = { onNavigateUp?.let { UpButton(it) } },
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.enterAlwaysScrollBehavior(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit
) {
    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = when (scrollBehavior) {
                null -> modifier
                else -> modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            },
            topBar = {
                TopAppBar(
                    title = { TopAppBarTitle(title, subtitle) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = floatingActionButton,
            content = content
        )
    }
}

@Composable
fun TopAppBarTitle(title: String, subtitle: String? = null) {
    Column {
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)

        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UpButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(painterResource(R.drawable.ic_symbol_arrow_back), contentDescription = stringResource(R.string.action_navigate_up))
    }
}
