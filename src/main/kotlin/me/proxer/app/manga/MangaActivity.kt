package me.proxer.app.manga

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.SingletonImageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.Size
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.ErrorState
import me.proxer.app.ui.components.LiveDataEffect
import me.proxer.app.ui.components.LoadingState
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.MediaControlInfo
import me.proxer.app.ui.components.MediaControlTexts
import me.proxer.app.ui.components.MediaControls
import me.proxer.app.ui.components.TopAppBarTitle
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.showErrorSnackbar
import me.proxer.app.ui.shell.AppNavigator
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Utils
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toLocalDateTimeBP
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

/**
 * The reader for a chapter of a manga. While reading, the system bars and the controls are hidden; tapping the center
 * of a page shows them.
 *
 * @author Ruben Gees
 */
class MangaActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"
        private const val CHAPTER_TITLE_EXTRA = "chapter_title"
        private const val NAME_EXTRA = "name"
        private const val EPISODE_AMOUNT_EXTRA = "episode_amount"

        private const val PRELOAD_PARALLELISM = 2

        fun navigateTo(
            context: Activity,
            id: String,
            episode: Int,
            language: Language,
            chapterTitle: String?,
            name: String? = null,
            episodeAmount: Int? = null
        ) {
            context.startActivity<MangaActivity>(
                ID_EXTRA to id,
                EPISODE_EXTRA to episode,
                LANGUAGE_EXTRA to language,
                CHAPTER_TITLE_EXTRA to chapterTitle,
                NAME_EXTRA to name,
                EPISODE_AMOUNT_EXTRA to episodeAmount
            )
        }
    }

    override val hasDarkSystemBars = true

    private val id: String
        get() = when (intent.hasExtra(ID_EXTRA)) {
            true -> intent.getSafeStringExtra(ID_EXTRA)
            false -> intent.data?.pathSegments?.getOrNull(1) ?: "-1"
        }

    private val initialEpisode: Int
        get() = when (intent.hasExtra(EPISODE_EXTRA)) {
            true -> intent.getIntExtra(EPISODE_EXTRA, 1)
            false -> intent.data?.pathSegments?.getOrNull(2)?.toIntOrNull() ?: 1
        }

    private val language: Language
        get() = when (intent.hasExtra(LANGUAGE_EXTRA)) {
            true -> intent.getSerializableExtra(LANGUAGE_EXTRA) as Language
            false ->
                intent.data?.pathSegments?.getOrNull(3)?.let { ProxerUtils.toApiEnum<Language>(it) }
                    ?: Language.ENGLISH
        }

    private val viewModel by viewModel<MangaViewModel> { parametersOf(id, language, initialEpisode) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val context = LocalContext.current
        val preferenceHelper = koinInject<PreferenceHelper>()
        val storageHelper = koinInject<StorageHelper>()
        val user by rememberCurrentUser()
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)
        val snackbarHostState = remember { SnackbarHostState() }

        var episode by rememberSaveable { mutableIntStateOf(viewModel.episode) }
        var orientation by remember { mutableStateOf(preferenceHelper.mangaReaderOrientation) }
        var areControlsVisible by rememberSaveable { mutableStateOf(true) }

        val chapterInfo = state.data
        val partialEntry = state.error?.data?.get(ErrorUtils.ENTRY_DATA_KEY) as? EntryCore
        val name = chapterInfo?.name ?: partialEntry?.name ?: intent.getStringExtra(NAME_EXTRA)
        val episodeAmount = chapterInfo?.episodeAmount ?: partialEntry?.episodeAmount
            ?: intent.getIntExtra(EPISODE_AMOUNT_EXTRA, Int.MAX_VALUE)
        val chapterTitle = chapterInfo?.chapter?.title?.takeIf { it.isNotBlank() }
            ?: state.error?.data?.get(ErrorUtils.CHAPTER_TITLE_DATA_KEY) as? String
            ?: intent.getStringExtra(CHAPTER_TITLE_EXTRA)

        fun switchEpisode(newEpisode: Int) {
            if (preferenceHelper.areBookmarksAutomatic && newEpisode > episode && user != null) {
                viewModel.bookmark(newEpisode)
            }

            episode = newEpisode
            areControlsVisible = true

            viewModel.setEpisode(newEpisode)
            intent.putExtra(EPISODE_EXTRA, newEpisode)
            intent.removeExtra(CHAPTER_TITLE_EXTRA)
        }

        ImmersiveMode(isImmersive = chapterInfo != null && !areControlsVisible)

        LaunchedEffect(chapterInfo?.chapter?.id) {
            if (chapterInfo != null) areControlsVisible = false
        }

        LiveDataEffect(viewModel.userStateData) {
            snackbarHostState.showSnackbar(context.getString(R.string.fragment_set_user_info_success))
        }

        LiveDataEffect(viewModel.userStateError) {
            snackbarHostState.showErrorSnackbar(
                context = context,
                error = it,
                onAction = onErrorAction,
                message = context.getString(R.string.error_set_user_info, context.getString(it.message))
            )
        }

        val controls: @Composable (showInfo: Boolean) -> Unit = { showInfo ->
            MediaControls(
                current = episode,
                amount = episodeAmount,
                texts = MediaControlTexts(
                    previous = stringResource(R.string.fragment_manga_previous_chapter),
                    next = stringResource(R.string.fragment_manga_next_chapter),
                    bookmarkThis = stringResource(R.string.fragment_manga_bookmark_this_chapter),
                    bookmarkNext = stringResource(R.string.fragment_manga_bookmark_next_chapter)
                ),
                onSwitch = ::switchEpisode,
                onBookmark = viewModel::bookmark,
                onFinish = viewModel::markAsFinished,
                info = if (showInfo && chapterInfo != null) chapterInfo.chapter.info(navigator) else emptyList()
            )
        }

        CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                val barPadding = WindowInsets.safeDrawing.asPaddingValues()

                when {
                    chapterInfo != null -> ChapterReader(
                        chapter = chapterInfo.chapter,
                        orientation = orientation,
                        episode = episode,
                        storageHelper = storageHelper,
                        onToggleControls = { areControlsVisible = !areControlsVisible },
                        header = { controls(true) },
                        footer = { controls(false) },
                        areControlsVisible = areControlsVisible
                    )
                    else -> Column(
                        Modifier
                            .fillMaxSize()
                            .padding(barPadding)
                            .padding(top = 64.dp)
                    ) {
                        if (partialEntry != null || state.error == null) {
                            Box(Modifier.padding(16.dp)) { controls(false) }
                        }

                        when (val error = state.error) {
                            null -> LoadingState(Modifier.weight(1f))
                            else -> ErrorState(error, onErrorAction, Modifier.weight(1f))
                        }
                    }
                }

                AnimatedVisibility(
                    visible = areControlsVisible || chapterInfo == null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    TopAppBar(
                        title = {
                            Column(
                                Modifier.clickable(
                                    enabled = name != null
                                ) { navigator.openMedia(id, name, Category.MANGA) }
                            ) {
                                val subtitle = chapterTitle ?: Category.MANGA.toEpisodeAppString(context, episode)

                                if (name == null) TopAppBarTitle(subtitle) else TopAppBarTitle(name, subtitle)
                            }
                        },
                        navigationIcon = { UpButton(navigator::navigateUp) },
                        actions = {
                            OrientationMenu(orientation) {
                                orientation = it
                                preferenceHelper.mangaReaderOrientation = it
                            }

                            if (name != null) {
                                IconButton(onClick = { navigator.share(shareText(name, chapterTitle, episode)) }) {
                                    Icon(
                                        painterResource(R.drawable.ic_symbol_share),
                                        contentDescription = stringResource(R.string.action_share)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)
                        )
                    )
                }

                SnackbarHost(
                    snackbarHostState,
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 80.dp)
                )
            }
        }
    }

    @Composable
    private fun ChapterReader(
        chapter: Chapter,
        orientation: MangaReaderOrientation,
        episode: Int,
        storageHelper: StorageHelper,
        areControlsVisible: Boolean,
        onToggleControls: () -> Unit,
        header: @Composable () -> Unit,
        footer: @Composable () -> Unit
    ) {
        val pages = chapter.pages.orEmpty()
        val itemCount = pages.size + 2
        val scope = rememberCoroutineScope()

        // The position includes the header, like the position stored by the previous versions of the reader.
        var position by rememberSaveable(chapter.id) {
            mutableIntStateOf((storageHelper.getLastMangaPage(id, episode, language) ?: 0).coerceIn(0, itemCount - 1))
        }

        // New states for each chapter and orientation, which start at the current position.
        val (pagerState, listState) = key(chapter.id, orientation) {
            rememberPagerState(initialPage = position) { itemCount } to rememberLazyListState(position)
        }

        LaunchedEffect(pagerState, listState, orientation) {
            snapshotFlow {
                when (orientation) {
                    MangaReaderOrientation.VERTICAL -> listState.firstVisibleItemIndex
                    else -> pagerState.currentPage
                }
            }.distinctUntilChanged().collect { position = it }
        }

        LaunchedEffect(chapter.id) { preload(chapter, (position - 1).coerceAtLeast(0)) }

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            if (position > 0 || storageHelper.getLastMangaPage(id, episode, language) != null) {
                storageHelper.putLastMangaPage(id, episode, language, position)
            }
        }

        val systemBarPadding = WindowInsets.safeDrawing.asPaddingValues()

        Box(Modifier.fillMaxSize()) {
            MangaReader(
                chapter = chapter,
                orientation = orientation,
                pagerState = pagerState,
                listState = listState,
                onToggleControls = onToggleControls,
                contentPadding = systemBarPadding,
                header = header,
                footer = footer
            )

            AnimatedVisibility(
                visible = areControlsVisible && pages.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                PageSlider(
                    page = position.coerceIn(1, pages.size),
                    pageCount = pages.size,
                    isRightToLeft = orientation == MangaReaderOrientation.RIGHT_TO_LEFT,
                    onPageChange = { page ->
                        scope.launch {
                            when (orientation) {
                                MangaReaderOrientation.VERTICAL -> listState.scrollToItem(page)
                                else -> pagerState.scrollToPage(page)
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * Downloads the pages of the [chapter] into the disk cache, beginning with the page at [startPage] (as the user is
     * most likely to read the pages after the current one next) and wrapping around to the pages before it.
     */
    private suspend fun preload(chapter: Chapter, startPage: Int) {
        val urls = chapter.pages.orEmpty().map { chapter.pageUrl(it) }
        val safeStartPage = startPage.coerceIn(0, (urls.size - 1).coerceAtLeast(0))
        val queue = Channel<String>(Channel.UNLIMITED)

        (urls.drop(safeStartPage) + urls.take(safeStartPage)).forEach { queue.trySend(it) }
        queue.close()

        val imageLoader = SingletonImageLoader.get(this)

        coroutineScope {
            repeat(PRELOAD_PARALLELISM) {
                launch {
                    for (url in queue) {
                        imageLoader.execute(
                            ImageRequest.Builder(this@MangaActivity)
                                .data(url)
                                .size(Size(1, 1))
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .build()
                        )
                    }
                }
            }
        }
    }

    private fun shareText(name: String, chapterTitle: String?, episode: Int): String {
        val link = ProxerUrls.mangaWeb(id, episode, language)

        return when {
            chapterTitle.isNullOrBlank() -> getString(R.string.share_manga, episode, name, link)
            else -> getString(R.string.share_manga_title, chapterTitle, name, link)
        }
    }

    private fun Chapter.info(navigator: AppNavigator): List<MediaControlInfo> {
        return listOfNotNull(
            MediaControlInfo(getString(R.string.view_media_control_uploader), uploaderName) {
                navigator.openProfile(uploaderId, uploaderName)
            },
            scanGroupId?.let { groupId ->
                scanGroupName?.let { groupName ->
                    MediaControlInfo(getString(R.string.view_media_control_translator_group), groupName) {
                        navigator.openTranslatorGroup(groupId, groupName)
                    }
                }
            },
            MediaControlInfo(
                getString(R.string.view_media_control_date),
                Utils.dateFormatter.format(date.toLocalDateTimeBP())
            )
        )
    }

    @Composable
    private fun ImmersiveMode(isImmersive: Boolean) {
        LaunchedEffect(isImmersive) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)

            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            if (isImmersive && !isInMultiWindowModeCompat) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
private fun OrientationMenu(
    orientation: MangaReaderOrientation,
    onOrientationChange: (MangaReaderOrientation) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }) {
            Icon(
                painterResource(R.drawable.ic_symbol_auto_stories),
                contentDescription = stringResource(R.string.fragment_manga_toggle_orientation)
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            listOf(
                MangaReaderOrientation.LEFT_TO_RIGHT to R.string.fragment_manga_left_to_right,
                MangaReaderOrientation.RIGHT_TO_LEFT to R.string.fragment_manga_right_to_left,
                MangaReaderOrientation.VERTICAL to R.string.fragment_manga_vertical
            ).forEach { (value, title) ->
                DropdownMenuItem(
                    text = { Text(stringResource(title)) },
                    leadingIcon = { RadioButton(selected = value == orientation, onClick = null) },
                    onClick = {
                        isExpanded = false

                        onOrientationChange(value)
                    }
                )
            }
        }
    }
}

@Composable
private fun PageSlider(page: Int, pageCount: Int, isRightToLeft: Boolean, onPageChange: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f)) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.fragment_manga_page, page, pageCount),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            if (pageCount > 1) {
                // Reading from right to left, the first page is on the right.
                val layoutDirection = if (isRightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Slider(
                        value = page.toFloat(),
                        onValueChange = { onPageChange(it.roundToInt()) },
                        valueRange = 1f..pageCount.toFloat(),
                        steps = (pageCount - 2).coerceAtLeast(0)
                    )
                }
            }
        }
    }
}

private val Activity.isInMultiWindowModeCompat
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode
