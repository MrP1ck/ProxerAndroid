package me.proxer.app.news

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import me.proxer.app.R
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.PagedStaggeredGrid
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.preview.PreviewData
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import me.proxer.app.ui.shell.AccountButton
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.library.entity.notifications.NewsArticle
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel

private const val COLLAPSED_DESCRIPTION_LINES = 3

/**
 * The news of Proxer as a feed of cards.
 */
@Composable
fun NewsScreen(viewModel: NewsViewModel = koinViewModel()) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    LifecycleResumeEffect(Unit) {
        NewsNotifications.cancel(context)

        onPauseOrDispose { }
    }

    ProxerScaffold(
        title = stringResource(R.string.section_news),
        actions = { AccountButton() }
    ) { padding ->
        NewsContent(
            state = state,
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            onArticleClick = { navigator.openTopic(it.threadId, it.categoryId, it.subject) },
            onImageClick = { navigator.openImage(ProxerUrls.newsImage(it.id, it.image)) },
            contentPadding = padding
        )
    }
}

@Composable
fun NewsContent(
    state: ContentState<List<NewsArticle>>,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onImageClick: (NewsArticle) -> Unit,
    contentPadding: PaddingValues
) {
    PagedStaggeredGrid(
        state = state,
        onLoadMore = onLoadMore,
        onRefresh = onRefresh,
        onErrorAction = onErrorAction,
        minColumnWidth = 320.dp,
        gridState = rememberLazyStaggeredGridState(),
        contentPadding = contentPadding.plus(PaddingValues(16.dp)),
        spacing = 12.dp,
        emptyMessage = R.string.error_no_data_news,
        key = { it.id },
        modifier = Modifier.fillMaxSize()
    ) { article ->
        NewsCard(article, onClick = { onArticleClick(article) }, onImageClick = { onImageClick(article) })
    }
}

@Composable
private fun NewsCard(article: NewsArticle, onClick: () -> Unit, onImageClick: () -> Unit) {
    val context = LocalContext.current
    var isExpanded by rememberSaveable(article.id) { mutableStateOf(false) }
    var isOverflowing by rememberSaveable(article.id) { mutableStateOf(false) }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ProxerAsyncImage(
            url = ProxerUrls.newsImage(article.id, article.image),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clickable(onClick = onImageClick)
        )

        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
            Text(
                text = article.category,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = article.subject,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = article.description.trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else COLLAPSED_DESCRIPTION_LINES,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!isExpanded) isOverflowing = it.hasVisualOverflow },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .animateContentSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = article.date.distanceInWordsToNow(context),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            if (isOverflowing || isExpanded) {
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_symbol_expand_more),
                        contentDescription = null,
                        modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                    )
                }
            } else {
                Box(Modifier.padding(vertical = 24.dp))
            }
        }
    }
}

@ThemePreviews
@Composable
private fun NewsContentPreview() = PreviewSurface {
    NewsContent(
        state = ContentState(PreviewData.newsArticles),
        onLoadMore = {},
        onRefresh = {},
        onErrorAction = {},
        onArticleClick = {},
        onImageClick = {},
        contentPadding = PaddingValues()
    )
}
