package me.proxer.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_DEFAULT
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE

private const val DEFAULT_PAGING_THRESHOLD = 5

/**
 * A list for [ContentState]s of paged data, with pull to refresh, loading more items when the end is reached and
 * errors at the end of the list. Replaces PagedContentFragment.
 */
@Composable
fun <T> PagedList(
    state: ContentState<List<T>>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    @StringRes emptyMessage: Int = R.string.error_no_data,
    key: ((T) -> Any)? = null,
    itemContent: @Composable LazyItemScope.(T) -> Unit
) {
    val isAtEnd by remember(listState) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= listState.layoutInfo.totalItemsCount - DEFAULT_PAGING_THRESHOLD
        }
    }

    PagedContainer(state, isAtEnd, onLoadMore, onRefresh, onErrorAction, modifier, contentPadding, emptyMessage) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = verticalArrangement,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(it.size, key = key?.let { keyOf -> { index -> keyOf(it[index]) } }) { index ->
                itemContent(it[index])
            }

            item(key = "footer", contentType = "footer") {
                PagedFooter(state, onErrorAction)
            }
        }
    }
}

/**
 * Like [PagedList], but lays out the items in a grid with columns of at least [minColumnWidth].
 */
@Composable
fun <T> PagedGrid(
    state: ContentState<List<T>>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    minColumnWidth: Dp,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(),
    spacing: Dp = 8.dp,
    @StringRes emptyMessage: Int = R.string.error_no_data,
    key: ((T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit
) {
    val isAtEnd by remember(gridState) {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisible >= gridState.layoutInfo.totalItemsCount - DEFAULT_PAGING_THRESHOLD
        }
    }

    PagedContainer(state, isAtEnd, onLoadMore, onRefresh, onErrorAction, modifier, contentPadding, emptyMessage) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minColumnWidth),
            state = gridState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(it.size, key = key?.let { keyOf -> { index -> keyOf(it[index]) } }) { index ->
                itemContent(it[index])
            }

            item(key = "footer", span = { GridItemSpan(maxLineSpan) }, contentType = "footer") {
                PagedFooter(state, onErrorAction)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PagedContainer(
    state: ContentState<List<T>>,
    isAtEnd: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    @StringRes emptyMessage: Int,
    content: @Composable (List<T>) -> Unit
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentState by rememberUpdatedState(state)
    val currentIsAtEnd by rememberUpdatedState(isAtEnd)
    var isRefreshRequested by rememberSaveable { mutableStateOf(false) }

    if (!state.isLoading) {
        isRefreshRequested = false
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            currentIsAtEnd && !currentState.isLoading && currentState.error == null && currentState.data != null
        }.collect { shouldLoadMore -> if (shouldLoadMore) currentOnLoadMore() }
    }

    RefreshErrorSnackbar(state.refreshError, onErrorAction)

    PullToRefreshBox(
        isRefreshing = isRefreshRequested && state.isLoading,
        onRefresh = {
            isRefreshRequested = true

            onRefresh()
        },
        modifier = modifier
    ) {
        ContentStateHost(
            state = state,
            onErrorAction = onErrorAction,
            contentPadding = contentPadding,
            emptyMessage = emptyMessage,
            content = content
        )
    }
}

@Composable
private fun PagedFooter(state: ContentState<*>, onErrorAction: (ErrorAction) -> Unit) {
    when {
        state.error != null -> InlineErrorState(state.error, onErrorAction)
        state.isLoading -> Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * Shows a snackbar when refreshing failed while data is shown, like PagedContentFragment did.
 */
@Composable
private fun RefreshErrorSnackbar(refreshError: ErrorAction?, onErrorAction: (ErrorAction) -> Unit) {
    val snackbarHostState = LocalSnackbarHostState.current ?: return
    val context = LocalContext.current

    LaunchedEffect(refreshError) {
        if (refreshError != null) {
            val actionLabel = when (refreshError.buttonMessage) {
                ACTION_MESSAGE_HIDE -> null
                ACTION_MESSAGE_DEFAULT -> context.getString(R.string.error_action_retry)
                else -> context.getString(refreshError.buttonMessage)
            }

            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.error_refresh, context.getString(refreshError.message)),
                actionLabel = actionLabel,
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed) {
                onErrorAction(refreshError)
            }
        }
    }
}
