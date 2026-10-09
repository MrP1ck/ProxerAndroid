package me.proxer.app.info

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.PagedGrid
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.RemovableCoverCard
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberLinkifiedText
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.library.enums.Category
import me.proxer.library.enums.Country
import okhttp3.HttpUrl
import java.util.Locale

/**
 * The info of an industry (e.g. a studio) or a translator group.
 */
data class OrganisationInfo(
    val name: String,
    val imageUrl: HttpUrl?,
    val rows: List<Pair<String, String>>,
    val link: HttpUrl?,
    val description: String
)

/**
 * An entry an industry or a translator group worked on.
 */
data class OrganisationProject(
    val id: String,
    val name: String,
    val subtitle: String,
    val category: Category,
    val rating: Float,
    val coverUrl: HttpUrl
)

/**
 * The screen of an industry or a translator group, with its info and projects in two tabs.
 */
@Composable
fun OrganisationScreen(
    initialName: String?,
    @StringRes shareText: Int,
    shareUrl: HttpUrl,
    infoState: ContentState<OrganisationInfo>,
    onInfoErrorAction: (ErrorAction) -> Unit,
    projectsState: ContentState<List<OrganisationProject>>,
    onProjectsErrorAction: (ErrorAction) -> Unit,
    onLoadMoreProjects: () -> Unit,
    onRefreshProjects: () -> Unit
) {
    val navigator = LocalAppNavigator.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 2 }
    val name = infoState.data?.name ?: initialName
    val shareMessage = name?.let { stringResource(shareText, it, shareUrl) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ProxerScaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = { UpButton(navigator::navigateUp) },
                    actions = {
                        if (shareMessage != null) {
                            IconButton(onClick = { navigator.share(shareMessage) }) {
                                Icon(painterResource(R.drawable.ic_symbol_share), stringResource(R.string.share_title))
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )

                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    listOf(R.string.section_industry_info, R.string.section_industry_projects).forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(stringResource(title)) }
                        )
                    }
                }
            }
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            when (page) {
                0 -> ContentStateHost(infoState, onInfoErrorAction, contentPadding = padding) { info ->
                    OrganisationInfoContent(info, padding)
                }
                else -> PagedGrid(
                    state = projectsState,
                    onLoadMore = onLoadMoreProjects,
                    onRefresh = onRefreshProjects,
                    onErrorAction = onProjectsErrorAction,
                    minColumnWidth = 128.dp,
                    contentPadding = padding.plus(PaddingValues(16.dp)),
                    spacing = 12.dp,
                    emptyMessage = R.string.error_no_data_projects,
                    key = { it.id }
                ) { project ->
                    RemovableCoverCard(
                        coverUrl = project.coverUrl,
                        title = project.name,
                        subtitle = project.subtitle,
                        rating = project.rating,
                        onClick = { navigator.openMedia(project.id, project.name, project.category) },
                        onRemove = null
                    )
                }
            }
        }
    }
}

@Composable
private fun OrganisationInfoContent(info: OrganisationInfo, contentPadding: PaddingValues) {
    val navigator = LocalAppNavigator.current

    LazyColumn(contentPadding = contentPadding.plus(PaddingValues(vertical = 16.dp))) {
        if (info.imageUrl != null) {
            item(key = "image") {
                ProxerAsyncImage(
                    url = info.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    showErrorIcon = false,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .aspectRatio(2f)
                        .clip(MaterialTheme.shapes.large)
                        .clickable { navigator.openImage(info.imageUrl) }
                )
            }
        }

        item(key = "rows") {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                info.rows.forEach { (title, content) ->
                    Row {
                        Text(
                            title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(112.dp)
                        )
                        Text(content, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (info.link != null) {
                    Row {
                        Text(
                            stringResource(R.string.fragment_about_website),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(112.dp)
                        )
                        Text(
                            rememberLinkifiedText(info.link.toString(), navigator::showPage),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (info.description.isNotBlank()) {
            item(key = "description") {
                SectionHeader(stringResource(R.string.fragment_media_info_description))

                SelectionContainer(Modifier.padding(horizontal = 16.dp)) {
                    Text(info.description.trim(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/**
 * The name of a country in German, or null if unknown.
 */
fun Country.displayName(): String? = when (this) {
    Country.GERMANY -> Locale("", "DE")
    Country.ENGLAND -> Locale("", "GB")
    Country.UNITED_STATES -> Locale("", "US")
    Country.JAPAN -> Locale("", "JP")
    Country.KOREA -> Locale("", "KR")
    Country.CHINA -> Locale("", "CN")
    Country.INTERNATIONAL, Country.OTHER, Country.NONE -> null
}?.getDisplayCountry(Locale.GERMAN)
