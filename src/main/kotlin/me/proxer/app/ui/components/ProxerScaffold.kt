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
 * The scaffold of a screen with a [TopAppBar] showing [title], which collapses on scroll. See the other overload for
 * the details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxerScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = TopAppBarDefaults.enterAlwaysScrollBehavior(),
    content: @Composable (PaddingValues) -> Unit
) {
    ProxerScaffold(
        topBar = {
            TopAppBar(
                title = { TopAppBarTitle(title, subtitle) },
                navigationIcon = { onNavigateUp?.let { UpButton(it) } },
                actions = actions,
                scrollBehavior = scrollBehavior
            )
        },
        modifier = modifier,
        floatingActionButton = floatingActionButton,
        scrollBehavior = scrollBehavior,
        content = content
    )
}

/**
 * The scaffold of a screen: an edge-to-edge [topBar], a snackbar host (exposed through [LocalSnackbarHostState], the
 * one of the app is used if there is one) and an optional floating action button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxerScaffold(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    floatingActionButton: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val snackbarHostState = LocalSnackbarHostState.current ?: remember { SnackbarHostState() }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = when (scrollBehavior) {
                null -> modifier
                else -> modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            },
            topBar = topBar,
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
        Icon(
            painter = painterResource(R.drawable.ic_symbol_arrow_back),
            contentDescription = stringResource(R.string.action_navigate_up)
        )
    }
}
