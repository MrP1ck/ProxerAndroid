package me.proxer.app.media.episode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.LanguageFlag
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.ButtonAction
import me.proxer.app.util.extension.toAnimeLanguage
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaLanguage
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The episodes or chapters of an entry. Tapping one shows its languages and hosters, tapping a language starts it.
 * If there is nothing yet, the user can set a bookmark to be notified.
 */
@Composable
fun EpisodeTab(
    entryId: String,
    name: String?,
    languages: Set<MediaLanguage>?,
    category: Category?,
    contentPadding: PaddingValues,
    viewModel: EpisodeViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by rememberCurrentUser()
    val state = viewModel.collectContentState()
    val retryHandler = rememberErrorActionHandler(viewModel::load)
    val bookmarkData by viewModel.bookmarkData.observeAsState()
    val bookmarkError by viewModel.bookmarkError.observeAsState()

    var isLanguagePickerVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(bookmarkData) {
        if (bookmarkData != null) snackbarHostState?.showSnackbar(context.getString(R.string.fragment_set_user_info_success))
    }

    LaunchedEffect(bookmarkError) {
        bookmarkError?.let {
            snackbarHostState?.showSnackbar(context.getString(R.string.error_set_user_info, context.getString(it.message)))
        }
    }

    val canBookmark = !languages.isNullOrEmpty() && category != null && user != null

    fun bookmark(language: MediaLanguage) {
        if (category != null) viewModel.bookmark(1, language, category)
    }

    val onErrorAction: (ErrorAction) -> Unit = { action ->
        when (action.buttonAction) {
            ButtonAction.BOOKMARK -> when (languages?.size) {
                1 -> bookmark(languages.first())
                else -> isLanguagePickerVisible = true
            }
            else -> retryHandler(action)
        }
    }

    // Without episodes, show the option to set a bookmark instead of the generic empty message.
    val displayedState = when {
        state.data?.isEmpty() == true -> state.copy(
            data = null,
            error = ErrorAction(
                if (category == Category.ANIME) R.string.error_no_data_episodes else R.string.error_no_data_chapters,
                if (canBookmark) R.string.fragment_media_info_bookmark else ErrorAction.ACTION_MESSAGE_HIDE,
                ButtonAction.BOOKMARK
            )
        )
        else -> state
    }

    ContentStateHost(state = displayedState, onErrorAction = onErrorAction, contentPadding = contentPadding) { episodes ->
        val listState = rememberLazyListState()
        var expandedEpisodes by rememberSaveable { mutableStateOf(emptySet<Int>()) }
        val isAtEnd by remember { derivedStateOf { !listState.canScrollForward } }

        Box(Modifier.fillMaxSize()) {
            LazyColumn(state = listState, contentPadding = contentPadding.plus(PaddingValues(bottom = 80.dp))) {
                items(episodes, key = { it.number }) { episode ->
                    EpisodeItem(
                        episode = episode,
                        isExpanded = episode.number in expandedEpisodes,
                        onClick = {
                            expandedEpisodes = when (episode.number in expandedEpisodes) {
                                true -> expandedEpisodes - episode.number
                                false -> expandedEpisodes + episode.number
                            }
                        },
                        onLanguageClick = { language ->
                            when (episode.category) {
                                Category.ANIME -> navigator.openAnimeEpisode(
                                    entryId,
                                    episode.number,
                                    language.toAnimeLanguage(),
                                    name,
                                    episode.episodeAmount
                                )
                                Category.MANGA, Category.NOVEL -> navigator.openMangaChapter(
                                    entryId,
                                    episode.number,
                                    language.toGeneralLanguage(),
                                    episode.title,
                                    name,
                                    episode.episodeAmount
                                )
                            }
                        }
                    )

                    HorizontalDivider()
                }
            }

            if (!isAtEnd) {
                SmallFloatingActionButton(
                    onClick = {
                        // Jump to the progress of the user first, then to the end.
                        val progressIndex = (episodes.firstOrNull()?.userProgress ?: 0) - 1
                        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val target = if (progressIndex > lastVisible) progressIndex else episodes.lastIndex

                        scope.launch { listState.scrollToItem(target.coerceAtLeast(0)) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = contentPadding.calculateBottomPadding())
                ) {
                    Icon(painterResource(R.drawable.ic_symbol_keyboard_double_arrow_down), contentDescription = null)
                }
            }
        }
    }

    if (isLanguagePickerVisible && languages != null) {
        LanguagePickerDialog(
            languages = languages,
            onSelect = {
                isLanguagePickerVisible = false
                bookmark(it)
            },
            onDismiss = { isLanguagePickerVisible = false }
        )
    }
}

@Composable
private fun EpisodeItem(
    episode: EpisodeRow,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onLanguageClick: (MediaLanguage) -> Unit
) {
    val context = LocalContext.current
    val isWatched = (episode.userProgress ?: 0) >= episode.number

    Column {
        ListItem(
            headlineContent = { Text(episode.title ?: episode.category.toEpisodeAppString(context, episode.number)) },
            leadingContent = if (isWatched) {
                {
                    Icon(
                        painterResource(R.drawable.ic_symbol_check_circle_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                { Icon(painterResource(R.drawable.ic_symbol_play_arrow), contentDescription = null) }
            },
            trailingContent = {
                Icon(
                    painterResource(R.drawable.ic_symbol_expand_more),
                    contentDescription = null,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                )
            },
            modifier = Modifier.clickable(onClick = onClick)
        )

        AnimatedVisibility(isExpanded) {
            Column(Modifier.padding(start = 56.dp, end = 16.dp, bottom = 8.dp)) {
                episode.languageHosterList.forEach { (language, hosterImages) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onLanguageClick(language) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LanguageFlag(language.toGeneralLanguage())

                        Text(
                            language.toAppString(context),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        hosterImages?.take(4)?.forEach { image ->
                            ProxerAsyncImage(
                                url = ProxerUrls.hosterImage(image),
                                contentDescription = null,
                                showErrorIcon = false,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(MaterialTheme.shapes.extraSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Lets the user choose the language of a bookmark.
 */
@Composable
fun LanguagePickerDialog(languages: Set<MediaLanguage>, onSelect: (MediaLanguage) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.fragment_episodes_bookmark_language_dialog_title)) },
        text = {
            Column {
                languages.forEach { language ->
                    ListItem(
                        headlineContent = { Text(language.toAppString(context)) },
                        leadingContent = { LanguageFlag(language.toGeneralLanguage()) },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(language) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
