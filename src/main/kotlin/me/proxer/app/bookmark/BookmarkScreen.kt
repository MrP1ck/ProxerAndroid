package me.proxer.app.bookmark

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.proxer.app.R
import me.proxer.app.ui.components.COVER_ASPECT_RATIO
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.LanguageFlag
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.PagedGrid
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SearchableTopAppBar
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.preview.PreviewData
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import me.proxer.app.ui.shell.AccountButton
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entity.ucp.Bookmark
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val SEARCH_DEBOUNCE_MILLIS = 500L

/**
 * The bookmarks of the user. Tapping a bookmark continues watching or reading, long pressing opens the details.
 * Bookmarks can be removed by swiping them away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(
    viewModel: BookmarkViewModel = koinViewModel(parameters = { parametersOf(null, null, false) })
) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    var isSearching by rememberSaveable { mutableStateOf(!viewModel.searchQuery.isNullOrBlank()) }
    var query by rememberSaveable { mutableStateOf(viewModel.searchQuery ?: "") }
    var category by rememberSaveable { mutableStateOf(viewModel.category) }
    var filterAvailable by rememberSaveable { mutableStateOf(viewModel.filterAvailable) }

    LaunchedEffect(query) {
        delay(SEARCH_DEBOUNCE_MILLIS)

        viewModel.searchQuery = query.trim()
    }

    val undoData by viewModel.undoData.observeAsState()
    val itemDeletionError by viewModel.itemDeletionError.observeAsState()
    val undoError by viewModel.undoError.observeAsState()

    LaunchedEffect(undoData) {
        if (undoData != null && snackbarHostState != null) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.fragment_bookmark_delete_message),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Long
            )

            if (result == SnackbarResult.ActionPerformed) viewModel.undo()
        }
    }

    LaunchedEffect(itemDeletionError) {
        itemDeletionError?.let {
            snackbarHostState?.showSnackbar(context.getString(R.string.error_bookmark_deletion, context.getString(it.message)))
        }
    }

    LaunchedEffect(undoError) {
        undoError?.let {
            snackbarHostState?.showSnackbar(context.getString(R.string.error_undo, context.getString(it.message)))
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    ProxerScaffold(
        topBar = {
            SearchableTopAppBar(
                title = stringResource(R.string.section_bookmarks),
                isSearching = isSearching,
                query = query,
                onQueryChange = { query = it },
                onSearchingChange = { searching ->
                    isSearching = searching

                    if (!searching) query = ""
                },
                searchHint = stringResource(R.string.bookmark_search_hint),
                actions = { AccountButton() },
                scrollBehavior = scrollBehavior
            )
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        BookmarkContent(
            state = state,
            category = category,
            filterAvailable = filterAvailable,
            isSearching = query.isNotBlank(),
            onCategoryChange = {
                category = it
                viewModel.category = it
            },
            onFilterAvailableChange = {
                filterAvailable = it
                viewModel.filterAvailable = it
            },
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            onBookmarkClick = { bookmark ->
                when (bookmark.category) {
                    Category.ANIME -> navigator.openAnimeEpisode(
                        bookmark.entryId,
                        bookmark.episode,
                        bookmark.language.toAnimeLanguage(),
                        bookmark.name
                    )
                    Category.MANGA, Category.NOVEL -> navigator.openMangaChapter(
                        bookmark.entryId,
                        bookmark.episode,
                        bookmark.language.toGeneralLanguage(),
                        bookmark.chapterName,
                        bookmark.name
                    )
                }
            },
            onBookmarkLongClick = { navigator.openMedia(it.entryId, it.name, it.category) },
            onBookmarkDelete = viewModel::addItemToDelete,
            contentPadding = padding
        )
    }
}

@Composable
fun BookmarkContent(
    state: ContentState<List<Bookmark>>,
    category: Category?,
    filterAvailable: Boolean,
    isSearching: Boolean,
    onCategoryChange: (Category?) -> Unit,
    onFilterAvailableChange: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkLongClick: (Bookmark) -> Unit,
    onBookmarkDelete: (Bookmark) -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = category == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text(stringResource(R.string.action_filter_all)) }
                )
            }

            item {
                FilterChip(
                    selected = category == Category.ANIME,
                    onClick = { onCategoryChange(Category.ANIME) },
                    label = { Text(stringResource(R.string.action_filter_anime)) }
                )
            }

            item {
                FilterChip(
                    selected = category == Category.MANGA,
                    onClick = { onCategoryChange(Category.MANGA) },
                    label = { Text(stringResource(R.string.action_filter_manga)) }
                )
            }

            item {
                FilterChip(
                    selected = filterAvailable,
                    onClick = { onFilterAvailableChange(!filterAvailable) },
                    label = { Text(stringResource(R.string.bookmark_filter_available)) }
                )
            }
        }

        PagedGrid(
            state = state,
            onLoadMore = onLoadMore,
            onRefresh = onRefresh,
            onErrorAction = onErrorAction,
            minColumnWidth = 340.dp,
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()).plus(
                PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
            ),
            emptyMessage = if (isSearching) R.string.error_no_data_search else R.string.error_no_data_bookmark,
            key = { it.id }
        ) { bookmark ->
            SwipeToDeleteBookmark(bookmark, onBookmarkDelete) {
                BookmarkCard(
                    bookmark = bookmark,
                    onClick = { onBookmarkClick(bookmark) },
                    onLongClick = { onBookmarkLongClick(bookmark) },
                    onDelete = { onBookmarkDelete(bookmark) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteBookmark(bookmark: Bookmark, onDelete: (Bookmark) -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled) onDelete(bookmark)

            it != SwipeToDismissBoxValue.Settled
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_bookmark_remove),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = { content() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkCard(bookmark: Bookmark, onClick: () -> Unit, onLongClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(R.string.bookmark_open_details)
            )
    ) {
        Row(Modifier.height(112.dp)) {
            ProxerAsyncImage(
                url = ProxerUrls.entryImage(bookmark.entryId),
                contentDescription = null,
                modifier = Modifier
                    .width(112.dp * COVER_ASPECT_RATIO)
                    .fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = bookmark.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = bookmark.medium.toAppString(context),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LanguageFlag(bookmark.language.toGeneralLanguage())

                    Text(
                        text = bookmark.chapterName ?: bookmark.category.toEpisodeAppString(context, bookmark.episode),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    AvailabilityIndicator(bookmark.isAvailable)
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterVertically)) {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_bookmark_remove),
                    contentDescription = stringResource(R.string.fragment_bookmark_delete_content_description)
                )
            }
        }
    }
}

@Composable
private fun AvailabilityIndicator(isAvailable: Boolean) {
    val description = stringResource(if (isAvailable) R.string.bookmark_available else R.string.bookmark_unavailable)

    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            .semanticsDescription(description)
    )
}

private fun Modifier.semanticsDescription(description: String) = this.then(
    Modifier.semantics { contentDescription = description }
)

@ThemePreviews
@Composable
private fun BookmarkContentPreview() = PreviewSurface {
    BookmarkContent(
        state = ContentState(PreviewData.bookmarks),
        category = null,
        filterAvailable = false,
        isSearching = false,
        onCategoryChange = {},
        onFilterAvailableChange = {},
        onLoadMore = {},
        onRefresh = {},
        onErrorAction = {},
        onBookmarkClick = {},
        onBookmarkLongClick = {},
        onBookmarkDelete = {},
        contentPadding = PaddingValues()
    )
}
