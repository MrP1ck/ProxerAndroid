package me.proxer.app.media

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.media.comments.CommentsTab
import me.proxer.app.media.discussion.DiscussionTab
import me.proxer.app.media.episode.EpisodeTab
import me.proxer.app.media.info.MediaInfoTab
import me.proxer.app.media.recommendation.RecommendationTab
import me.proxer.app.media.relation.RelationTab
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The tabs of the media screen. Novels have no episodes tab.
 */
enum class MediaTab(@StringRes val title: Int) {
    INFO(R.string.section_media_info),
    COMMENTS(R.string.section_comments),
    EPISODES(R.string.category_anime_episodes_title),
    RELATIONS(R.string.section_relations),
    RECOMMENDATIONS(R.string.section_recommendations),
    DISCUSSIONS(R.string.section_discussions);

    companion object {
        fun forCategory(category: Category?) = when (category) {
            null -> listOf(INFO)
            Category.ANIME, Category.MANGA -> values().toList()
            Category.NOVEL -> values().filter { it != EPISODES }
        }
    }
}

/**
 * The details of an anime, manga or novel: the info, comments, episodes, relations, recommendations and discussions in
 * tabs.
 */
@Composable
fun MediaScreen(
    entryId: String,
    initialName: String?,
    initialCategory: Category?,
    initialTab: MediaTab,
    viewModel: MediaInfoViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val infoState = viewModel.collectContentState()
    val entry = infoState.data

    val name = entry?.name ?: initialName
    val category = entry?.category ?: initialCategory
    val tabs = MediaTab.forCategory(category)

    val pagerState = rememberPagerState { tabs.size }
    var hasShownInitialTab by rememberSaveable { mutableStateOf(false) }

    // Deep links can point to a tab, which is only available once the category is known.
    LaunchedEffect(tabs) {
        if (!hasShownInitialTab && initialTab in tabs) {
            hasShownInitialTab = true

            pagerState.scrollToPage(tabs.indexOf(initialTab))
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ProxerScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { UpButton(navigator::navigateUp) },
                    actions = {
                        if (name != null) {
                            IconButton(
                                onClick = {
                                    navigator.share(
                                        context.getString(R.string.share_media, name, ProxerUrls.infoWeb(entryId))
                                    )
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_symbol_share),
                                    contentDescription = stringResource(R.string.share_title)
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )

                if (tabs.size > 1) {
                    PrimaryScrollableTabRow(selectedTabIndex = pagerState.currentPage.coerceAtMost(tabs.lastIndex)) {
                        tabs.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                text = {
                                    Text(
                                        when {
                                            tab == MediaTab.EPISODES && category != Category.ANIME -> {
                                                stringResource(R.string.category_manga_episodes_title)
                                            }
                                            else -> stringResource(tab.title)
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (tabs.getOrNull(page)) {
                MediaTab.INFO, null -> MediaInfoTab(viewModel, infoState, padding)
                MediaTab.COMMENTS -> CommentsTab(entryId, name, category, padding)
                MediaTab.EPISODES -> EpisodeTab(entryId, name, entry?.languages, category, padding)
                MediaTab.RELATIONS -> RelationTab(entryId, padding)
                MediaTab.RECOMMENDATIONS -> RecommendationTab(entryId, padding)
                MediaTab.DISCUSSIONS -> DiscussionTab(entryId, padding)
            }
        }
    }
}
