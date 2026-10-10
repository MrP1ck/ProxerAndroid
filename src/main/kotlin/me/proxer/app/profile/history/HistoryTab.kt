package me.proxer.app.profile.history

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.PagedGrid
import me.proxer.app.ui.components.RemovableCoverCard
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The episodes and chapters a user watched or read recently. Tapping one continues there.
 */
@Composable
fun HistoryTab(
    userId: String?,
    username: String?,
    contentPadding: PaddingValues,
    viewModel: HistoryViewModel = koinViewModel(parameters = { parametersOf(userId, username) })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    PagedGrid(
        state = state,
        onLoadMore = viewModel::loadIfPossible,
        onRefresh = viewModel::refresh,
        onErrorAction = onErrorAction,
        minColumnWidth = 128.dp,
        contentPadding = contentPadding.plus(PaddingValues(16.dp)),
        spacing = 12.dp,
        emptyMessage = R.string.error_no_data_history,
        key = { it.id }
    ) { entry ->
        val time = (entry as? LocalUserHistoryEntry.Ucp)?.date?.distanceInWordsToNow(context)
        val episode = entry.category.toEpisodeAppString(context, entry.episode)

        RemovableCoverCard(
            coverUrl = ProxerUrls.entryImage(entry.entryId),
            title = entry.name,
            subtitle = if (time != null) "$episode · $time" else episode,
            onClick = {
                when (entry.category) {
                    Category.ANIME -> navigator.openAnimeEpisode(
                        entry.entryId,
                        entry.episode,
                        entry.language.toAnimeLanguage(),
                        entry.name
                    )
                    Category.MANGA, Category.NOVEL -> navigator.openMangaChapter(
                        entry.entryId,
                        entry.episode,
                        entry.language.toGeneralLanguage(),
                        null,
                        entry.name
                    )
                }
            },
            onRemove = null
        )
    }
}
