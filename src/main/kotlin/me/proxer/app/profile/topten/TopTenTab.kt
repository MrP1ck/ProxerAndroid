package me.proxer.app.profile.topten

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.LiveDataEffect
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.RemovableCoverCard
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.toAppString
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The favorite anime and manga of a user. Users can remove entries from their own favorites.
 */
@Composable
fun TopTenTab(
    userId: String?,
    username: String?,
    isOwnProfile: Boolean,
    contentPadding: PaddingValues,
    viewModel: TopTenViewModel = koinViewModel(parameters = { parametersOf(userId, username) })
) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    LiveDataEffect(viewModel.itemDeletionError) {
        snackbarHostState?.showSnackbar(
            context.getString(R.string.error_topten_entry_removal, context.getString(it.message))
        )
    }

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_top_ten,
        isEmpty = { it.animeEntries.isEmpty() && it.mangaEntries.isEmpty() }
    ) { result ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            contentPadding = contentPadding.plus(PaddingValues(16.dp)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            listOf(
                R.string.section_user_media_list_anime to result.animeEntries,
                R.string.section_user_media_list_manga to result.mangaEntries
            ).forEach { (title, entries) ->
                if (entries.isNotEmpty()) {
                    item(key = "header_$title", span = { GridItemSpan(maxLineSpan) }) {
                        SectionHeader(stringResource(title))
                    }

                    items(entries, key = { it.id }) { entry ->
                        val entryId = if (entry is LocalTopTenEntry.Ucp) entry.entryId else entry.id

                        RemovableCoverCard(
                            coverUrl = ProxerUrls.entryImage(entryId),
                            title = entry.name,
                            subtitle = entry.medium.toAppString(context),
                            onClick = { navigator.openMedia(entryId, entry.name, entry.category) },
                            onRemove = if (isOwnProfile && entry is LocalTopTenEntry.Ucp) {
                                { viewModel.addItemToDelete(entry) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}
