package me.proxer.app.profile.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.PagedGrid
import me.proxer.app.ui.components.RemovableCoverCard
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The anime or manga list of a user, filterable by progress. Users can remove entries from their own list.
 */
@Composable
fun ProfileMediaListTab(
    userId: String?,
    username: String?,
    category: Category,
    isOwnProfile: Boolean,
    contentPadding: PaddingValues,
    viewModel: ProfileMediaListViewModel = koinViewModel(
        key = "profile_media_list_$category",
        parameters = { parametersOf(userId, username, category, null) }
    )
) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)
    val deletionError by viewModel.itemDeletionError.observeAsState()
    var filter by rememberSaveable { mutableStateOf(viewModel.filter) }

    LaunchedEffect(deletionError) {
        deletionError?.let {
            snackbarHostState?.showSnackbar(
                context.getString(R.string.error_media_entry_deletion, context.getString(it.message))
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        val filters = listOf(
            null to R.string.media_filter_all,
            UserMediaListFilterType.WATCHING to when (category == Category.ANIME) {
                true -> R.string.media_filter_watching
                false -> R.string.media_filter_reading
            },
            UserMediaListFilterType.WATCHED to R.string.media_filter_finished,
            UserMediaListFilterType.WILL_WATCH to when (category == Category.ANIME) {
                true -> R.string.media_filter_will_watch
                false -> R.string.media_filter_will_read
            },
            UserMediaListFilterType.CANCELLED to R.string.media_filter_cancelled
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters, key = { it.second }) { (type, label) ->
                FilterChip(
                    selected = filter == type,
                    onClick = {
                        filter = type
                        viewModel.filter = type
                    },
                    label = { Text(stringResource(label)) }
                )
            }
        }

        PagedGrid(
            state = state,
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            minColumnWidth = 128.dp,
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()).plus(
                PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ),
            spacing = 12.dp,
            emptyMessage = R.string.error_no_data_user_media_list,
            key = { it.id }
        ) { entry ->
            RemovableCoverCard(
                coverUrl = ProxerUrls.entryImage(entry.id),
                title = entry.name,
                subtitle = entry.mediaProgress.toEpisodeAppString(context, entry.episode, entry.medium.toCategory()),
                rating = entry.rating.toFloat(),
                onClick = { navigator.openMedia(entry.id, entry.name, entry.medium.toCategory()) },
                onRemove = if (isOwnProfile && entry is LocalUserMediaListEntry.Ucp) {
                    { viewModel.addItemToDelete(entry) }
                } else {
                    null
                }
            )
        }
    }
}
