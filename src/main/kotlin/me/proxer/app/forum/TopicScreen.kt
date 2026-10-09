package me.proxer.app.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.BBCodeText
import me.proxer.app.ui.components.PagedList
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.TopAppBarTitle
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.library.enums.Device
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The posts of a forum thread.
 */
@Composable
fun TopicScreen(id: String, categoryId: String, initialTopic: String?, deepLinkUrl: String?) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val viewModel = koinViewModel<TopicViewModel> { parametersOf(id, context.resources) }
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)
    val metaData by viewModel.metaData.observeAsState()
    val topic = metaData?.subject ?: initialTopic
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    ProxerScaffold(
        topBar = {
            TopAppBar(
                title = { TopAppBarTitle(topic ?: "", metaData?.categoryName) },
                navigationIcon = { UpButton(navigator::navigateUp) },
                actions = {
                    IconButton(
                        onClick = {
                            val webUrl = deepLinkUrl?.toPrefixedUrlOrNull()
                                ?.newBuilder()
                                ?.setQueryParameter("device", ProxerUtils.getSafeApiEnumName(Device.MOBILE))
                                ?.build()
                                ?: ProxerUrls.forumWeb(metaData?.categoryId ?: categoryId, id, Device.MOBILE)

                            navigator.showPage(webUrl, forceBrowser = true, skipCheck = true)
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_symbol_open_in_browser),
                            contentDescription = stringResource(R.string.action_show_in_browser)
                        )
                    }

                    if (topic != null) {
                        IconButton(
                            onClick = {
                                val url = deepLinkUrl ?: ProxerUrls.forumWeb(categoryId, id).toString()

                                navigator.share(context.getString(R.string.share_topic, topic, url))
                            }
                        ) {
                            Icon(painterResource(R.drawable.ic_symbol_share), stringResource(R.string.share_title))
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        PagedList(
            state = state,
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            contentPadding = padding.plus(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            key = { it.id }
        ) { post ->
            PostCard(post, onAuthorClick = { navigator.openProfile(post.userId, post.username, post.image) })
        }
    }
}

@Composable
private fun PostCard(post: ParsedPost, onAuthorClick: () -> Unit) {
    val context = LocalContext.current

    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onAuthorClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (post.image.isBlank()) {
                    Icon(
                        painterResource(R.drawable.ic_symbol_person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                } else {
                    ProxerAsyncImage(ProxerUrls.userImage(post.image), null, Modifier.size(40.dp), showErrorIcon = false)
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(post.username, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    post.date.distanceInWordsToNow(context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (post.thankYouAmount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painterResource(R.drawable.ic_symbol_thumb_up), contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(post.thankYouAmount.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        BBCodeText(
            tree = post.parsedMessage,
            userId = post.userId,
            enableEmoticons = true,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        post.signature?.let { signature ->
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            BBCodeText(
                tree = signature,
                userId = post.userId,
                enableEmoticons = true,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
