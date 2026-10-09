package me.proxer.app.manga

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.request.ImageRequest
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.util.extension.decodedName
import me.proxer.library.entity.manga.Chapter
import me.proxer.library.entity.manga.Page
import me.proxer.library.util.ProxerUrls
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

private const val TAP_ZONE_FRACTION = 4
private const val DEFAULT_PAGE_ASPECT_RATIO = 0.7f

/**
 * The pages of a [chapter] with the [header] before and the [footer] after them. The pages are read in a pager
 * (left to right or right to left) or a list (vertical, which suits webtoons) depending on the [orientation].
 *
 * Tapping the edges moves to the previous or next page, tapping the center calls [onToggleControls]. The items are the
 * header, the pages and the footer, so the index of the first page is 1. [pagerState] and [listState] must have
 * `pages + 2` items.
 */
@Composable
fun MangaReader(
    chapter: Chapter,
    orientation: MangaReaderOrientation,
    pagerState: PagerState,
    listState: LazyListState,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    pageUrl: (Page) -> String = chapter::pageUrl,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit
) {
    val pages = chapter.pages.orEmpty()
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }

        when (orientation) {
            MangaReaderOrientation.VERTICAL -> {
                val onTap: (Offset) -> Unit = { offset ->
                    when {
                        offset.y < height / TAP_ZONE_FRACTION -> scope.launch { listState.animateScrollBy(-height / 2) }
                        offset.y > height - height / TAP_ZONE_FRACTION ->
                            scope.launch { listState.animateScrollBy(height / 2) }
                        else -> onToggleControls()
                    }
                }

                LazyColumn(state = listState, contentPadding = contentPadding, modifier = Modifier.fillMaxSize()) {
                    item(key = "header") { ControlsPage(isFullHeight = false) { header() } }

                    itemsIndexed(pages, key = { _, page -> page.name }) { _, page ->
                        MangaPage(
                            url = pageUrl(page),
                            isVertical = true,
                            onTap = onTap,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(page.aspectRatio())
                        )
                    }

                    item(key = "footer") { ControlsPage(isFullHeight = false) { footer() } }
                }
            }
            else -> {
                val isRightToLeft = orientation == MangaReaderOrientation.RIGHT_TO_LEFT

                val onTap: (Offset) -> Unit = { offset ->
                    val isStart = offset.x < width / TAP_ZONE_FRACTION
                    val isEnd = offset.x > width - width / TAP_ZONE_FRACTION

                    when {
                        isStart || isEnd -> {
                            val isForward = if (isRightToLeft) isStart else isEnd
                            val target = pagerState.currentPage + if (isForward) 1 else -1

                            scope.launch { pagerState.animateScrollToPage(target.coerceIn(0, pagerState.pageCount - 1)) }
                        }
                        else -> onToggleControls()
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    reverseLayout = isRightToLeft,
                    beyondViewportPageCount = 1,
                    key = { index -> pages.getOrNull(index - 1)?.name ?: "controls_$index" },
                    modifier = Modifier.fillMaxSize()
                ) { index ->
                    when (index) {
                        0 -> ControlsPage(isFullHeight = true, contentPadding = contentPadding) { header() }
                        pages.size + 1 -> ControlsPage(isFullHeight = true, contentPadding = contentPadding) { footer() }
                        else -> MangaPage(
                            url = pageUrl(pages[index - 1]),
                            isVertical = false,
                            onTap = onTap,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsPage(
    isFullHeight: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isFullHeight) it.fillMaxSize() else it }
            .padding(contentPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.widthIn(max = 600.dp)) { content() }
    }
}

/**
 * A zoomable page. Large pages are decoded in tiles (sub-sampled) by telephoto, using the platform decoders, which
 * also support WebP. [onTap] receives the position in the root.
 */
@Composable
private fun MangaPage(url: String, isVertical: Boolean, onTap: (Offset) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var retries by remember(url) { mutableIntStateOf(0) }
    var isError by remember(url, retries) { mutableStateOf(false) }
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val request = remember(url, retries) {
        ImageRequest.Builder(context)
            .data(url)
            .listener(onError = { _, _ -> isError = true })
            .build()
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .onGloballyPositioned { coordinates = it },
        contentAlignment = Alignment.Center
    ) {
        if (isError) {
            IconButton(onClick = { retries++ }) {
                Icon(painterResource(R.drawable.ic_symbol_refresh), contentDescription = null)
            }
        } else {
            val state = rememberZoomableImageState(rememberZoomableState())

            ZoomableAsyncImage(
                model = request,
                contentDescription = null,
                state = state,
                contentScale = if (isVertical) ContentScale.FillWidth else ContentScale.Fit,
                onClick = { offset -> coordinates?.let { onTap(it.localToRoot(offset)) } },
                modifier = Modifier.fillMaxSize()
            )

            if (!state.isImageDisplayed) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

internal fun Chapter.pageUrl(page: Page) = ProxerUrls.mangaPageImage(server, entryId, id, page.decodedName).toString()

private fun Page.aspectRatio() = when {
    width > 0 && height > 0 -> width.toFloat() / height.toFloat()
    else -> DEFAULT_PAGE_ASPECT_RATIO
}
