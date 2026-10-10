package me.proxer.app.media.relation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.MediaCoverCard
import me.proxer.app.ui.components.RatingBadge
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.toAppString
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The entries related to an entry, like sequels or adaptions.
 */
@Composable
fun RelationTab(
    entryId: String,
    contentPadding: PaddingValues,
    viewModel: RelationViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_relations
    ) { relations ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(144.dp),
            contentPadding = contentPadding.plus(PaddingValues(16.dp)),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(relations, key = { it.id }) { relation ->
                MediaCoverCard(
                    coverUrl = ProxerUrls.entryImage(relation.id),
                    title = relation.name,
                    subtitle = relation.medium.toAppString(context) + " · " + pluralStringResource(
                        when (relation.category == Category.ANIME) {
                            true -> R.plurals.media_episode_count
                            false -> R.plurals.media_chapter_count
                        },
                        relation.episodeAmount,
                        relation.episodeAmount
                    ),
                    onClick = { navigator.openMedia(relation.id, relation.name, relation.category) },
                    overlay = {
                        if (relation.rating > 0) {
                            Row(Modifier.padding(8.dp)) { RatingBadge(relation.rating) }
                        }
                    }
                )
            }
        }
    }
}
