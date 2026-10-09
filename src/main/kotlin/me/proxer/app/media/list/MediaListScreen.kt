package me.proxer.app.media.list

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.proxer.app.R
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.LanguageFlag
import me.proxer.app.ui.components.MediaCoverCard
import me.proxer.app.ui.components.PagedGrid
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.RatingBadge
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
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.entity.list.MediaListEntry
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The anime or manga list with search, type filter, sorting and the extended search criteria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListScreen(
    category: Category,
    initialType: String?,
    initialSort: String?,
    viewModel: MediaListViewModel = koinViewModel(
        key = "media_list_$category",
        parameters = { parametersOf(MediaListFilter.initial(category, initialType, initialSort)) }
    )
) {
    val navigator = LocalAppNavigator.current
    val state = viewModel.collectContentState()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val genres by viewModel.genreData.observeAsState()
    val tags by viewModel.tagData.observeAsState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    var isSearching by rememberSaveable { mutableStateOf(!filter.searchQuery.isNullOrBlank()) }
    var query by rememberSaveable { mutableStateOf(filter.searchQuery ?: "") }
    var isFilterSheetVisible by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    ProxerScaffold(
        topBar = {
            SearchableTopAppBar(
                title = stringResource(if (category == Category.ANIME) R.string.section_anime else R.string.section_manga),
                isSearching = isSearching,
                query = query,
                onQueryChange = { query = it },
                onSearchingChange = { searching ->
                    isSearching = searching

                    if (!searching) {
                        query = ""
                        viewModel.updateFilter(filter.copy(searchQuery = null))
                    }
                },
                searchHint = stringResource(R.string.media_list_search_hint),
                onSearch = { viewModel.updateFilter(filter.copy(searchQuery = query.trim())) },
                actions = {
                    SortMenu(filter.sortCriteria) { viewModel.updateFilter(filter.copy(sortCriteria = it)) }

                    if (category == Category.ANIME) {
                        IconButton(onClick = navigator::openSchedule) {
                            Icon(
                                painterResource(R.drawable.ic_symbol_calendar_month),
                                contentDescription = stringResource(R.string.section_schedule)
                            )
                        }
                    }

                    AccountButton()
                },
                scrollBehavior = scrollBehavior
            )
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        MediaListContent(
            category = category,
            state = state,
            filter = filter,
            onTypeChange = { viewModel.updateFilter(filter.copy(type = it)) },
            onFilterClick = {
                if (genres == null || tags == null) viewModel.loadTags()

                isFilterSheetVisible = true
            },
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            onEntryClick = { navigator.openMedia(it.id, it.name, it.medium.toCategory()) },
            contentPadding = padding
        )
    }

    if (isFilterSheetVisible) {
        MediaListFilterSheet(
            filter = filter,
            genres = genres,
            tags = tags,
            onApply = { viewModel.updateFilter(it) },
            onDismiss = { isFilterSheetVisible = false }
        )
    }
}

@Composable
fun MediaListContent(
    category: Category,
    state: ContentState<List<MediaListEntry>>,
    filter: MediaListFilter,
    onTypeChange: (MediaType) -> Unit,
    onFilterClick: () -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    onEntryClick: (MediaListEntry) -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        TypeChips(category, filter, onTypeChange, onFilterClick)

        PagedGrid(
            state = state,
            onLoadMore = onLoadMore,
            onRefresh = onRefresh,
            onErrorAction = onErrorAction,
            minColumnWidth = 144.dp,
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()).plus(
                PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
            ),
            spacing = 12.dp,
            emptyMessage = R.string.error_no_data_search,
            key = { it.id }
        ) { entry ->
            MediaEntryCard(entry, category, onClick = { onEntryClick(entry) })
        }
    }
}

@Composable
private fun TypeChips(
    category: Category,
    filter: MediaListFilter,
    onTypeChange: (MediaType) -> Unit,
    onFilterClick: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "filter") {
            FilterChip(
                selected = filter.extendedCriteriaCount > 0,
                onClick = onFilterClick,
                label = { Text(stringResource(R.string.action_filter)) },
                leadingIcon = {
                    BadgedBox(
                        badge = {
                            if (filter.extendedCriteriaCount > 0) Badge { Text(filter.extendedCriteriaCount.toString()) }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_symbol_tune),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        }

        items(MediaListFilter.typesFor(category), key = { it.name }) { type ->
            FilterChip(
                selected = filter.type == type,
                onClick = { onTypeChange(type) },
                label = { Text(stringResource(type.label())) }
            )
        }
    }
}

@Composable
private fun SortMenu(current: MediaSearchSortCriteria, onSelect: (MediaSearchSortCriteria) -> Unit) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(painterResource(R.drawable.ic_symbol_sort), contentDescription = stringResource(R.string.action_sort))
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            MediaListFilter.sortCriteria.forEach { criteria ->
                DropdownMenuItem(
                    text = { Text(stringResource(criteria.label())) },
                    leadingIcon = { RadioButton(selected = criteria == current, onClick = null) },
                    onClick = {
                        isExpanded = false
                        onSelect(criteria)
                    }
                )
            }
        }
    }
}

@Composable
private fun MediaEntryCard(entry: MediaListEntry, category: Category, onClick: () -> Unit) {
    val context = LocalContext.current
    val languages = remember(entry) { entry.languages.map { it.toGeneralLanguage() }.distinct() }

    MediaCoverCard(
        coverUrl = ProxerUrls.entryImage(entry.id),
        title = entry.name,
        subtitle = entry.medium.toAppString(context) + " · " + pluralStringResource(
            if (category == Category.ANIME) R.plurals.media_episode_count else R.plurals.media_chapter_count,
            entry.episodeAmount,
            entry.episodeAmount
        ),
        onClick = onClick,
        overlay = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (entry.rating > 0) {
                    RatingBadge(entry.rating)
                } else {
                    Box(Modifier)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    languages.filter { it != Language.OTHER }.forEach { LanguageFlag(it) }
                }
            }
        }
    )
}

@StringRes
private fun MediaType.label() = when (this) {
    MediaType.ALL_ANIME, MediaType.ALL_MANGA, MediaType.ALL, MediaType.ALL_WITH_HENTAI -> R.string.action_filter_all
    MediaType.ANIMESERIES -> R.string.action_filter_anime_series
    MediaType.MOVIE -> R.string.action_filter_movies
    MediaType.OVA -> R.string.action_filter_ova
    MediaType.HENTAI -> R.string.action_filter_hentai
    MediaType.MANGASERIES -> R.string.action_filter_manga_series
    MediaType.ONESHOT -> R.string.action_filter_one_shots
    MediaType.DOUJIN -> R.string.action_filter_doujin
    MediaType.HMANGA -> R.string.action_filter_hmanga
}

@StringRes
private fun MediaSearchSortCriteria.label() = when (this) {
    MediaSearchSortCriteria.RATING -> R.string.action_sort_rating
    MediaSearchSortCriteria.CLICKS -> R.string.action_sort_clicks
    MediaSearchSortCriteria.EPISODE_AMOUNT -> R.string.action_sort_count
    MediaSearchSortCriteria.NAME, MediaSearchSortCriteria.RELEVANCE -> R.string.action_sort_name
}

@ThemePreviews
@Composable
private fun MediaListContentPreview() = PreviewSurface {
    MediaListContent(
        category = Category.ANIME,
        state = ContentState(PreviewData.mediaListEntries),
        filter = MediaListFilter(MediaType.ALL_ANIME, hideFinished = true),
        onTypeChange = {},
        onFilterClick = {},
        onLoadMore = {},
        onRefresh = {},
        onErrorAction = {},
        onEntryClick = {},
        contentPadding = PaddingValues()
    )
}
