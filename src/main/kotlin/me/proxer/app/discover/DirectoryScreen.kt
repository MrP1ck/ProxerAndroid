package me.proxer.app.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.proxer.app.R
import me.proxer.app.info.ProfileListItem
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.PagedList
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SearchableTopAppBar
import me.proxer.app.ui.components.UpButton
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.toAppString

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private val LETTERS = ('A'..'Z').map { it.toString() }

/**
 * An alphabetical, searchable directory. Selecting a letter filters by the first letter of the name, a search query
 * replaces it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    kind: DirectoryKind,
    state: ContentState<List<DirectoryEntry>>,
    onFilterChange: (letter: String?, searchQuery: String?) -> Unit,
    onEntryClick: (DirectoryEntry) -> Unit,
    onNavigateUp: () -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit
) {
    val context = LocalContext.current

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var letter by rememberSaveable { mutableStateOf<String?>(null) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(letter, query) {
        // Letters are applied immediately, typing is debounced.
        if (query.isNotBlank()) delay(SEARCH_DEBOUNCE_MILLIS)

        onFilterChange(letter, query)
    }

    ProxerScaffold(
        topBar = {
            SearchableTopAppBar(
                title = stringResource(kind.title),
                isSearching = isSearching,
                query = query,
                onQueryChange = { query = it },
                onSearchingChange = { searching ->
                    isSearching = searching

                    if (!searching) query = ""
                },
                searchHint = stringResource(kind.searchHint),
                navigationIcon = { UpButton(onNavigateUp) },
                scrollBehavior = scrollBehavior
            )
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            LetterChips(letter, onLetterChange = { letter = it })

            PagedList(
                state = state,
                onLoadMore = onLoadMore,
                onRefresh = onRefresh,
                onErrorAction = onErrorAction,
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding()),
                key = { it.id }
            ) { entry ->
                ProfileListItem(
                    title = entry.name,
                    subtitle = entry.industryType?.toAppString(context),
                    imageUrl = entry.imageUrl,
                    onClick = { onEntryClick(entry) }
                )
            }
        }
    }
}

@Composable
private fun LetterChips(letter: String?, onLetterChange: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all") {
            FilterChip(
                selected = letter == null,
                onClick = { onLetterChange(null) },
                label = { Text(stringResource(R.string.directory_all)) }
            )
        }

        items(LETTERS, key = { it }) { item ->
            FilterChip(selected = letter == item, onClick = { onLetterChange(item) }, label = { Text(item) })
        }
    }
}
