package me.proxer.app.info

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.ui.components.BBCodeText
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.InfoRow
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.view.bbcode.toBBTree
import me.proxer.app.util.ErrorUtils.ErrorAction
import okhttp3.HttpUrl

/**
 * The details of a character or a person.
 *
 * @property facts The facts like the birthday, as pairs of the title and the content.
 * @property names The other names, as pairs of the title and the content.
 * @property descriptions The sections of the description with BBCode, as pairs of the title and the text.
 * @property groups The related entries, characters or persons.
 */
data class ProfileDetails(
    val name: String,
    val imageUrl: HttpUrl,
    val facts: List<Pair<String, String>>,
    val names: List<Pair<String, String>>,
    val descriptions: List<Pair<String, String>>,
    val groups: List<ProfileGroup>
)

data class ProfileGroup(val title: String, val items: List<ProfileLink>)

data class ProfileLink(
    val key: String,
    val title: String,
    val subtitle: String?,
    val imageUrl: HttpUrl?,
    val onClick: () -> Unit
)

/**
 * The screen of a character or a person.
 */
@Composable
fun ProfileScreen(
    initialName: String?,
    @StringRes shareText: Int,
    shareUrl: HttpUrl,
    state: ContentState<ProfileDetails>,
    onErrorAction: (ErrorAction) -> Unit
) {
    val navigator = LocalAppNavigator.current
    val name = state.data?.name ?: initialName
    val shareMessage = name?.let { stringResource(shareText, it, shareUrl) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    ProxerScaffold(
        topBar = {
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
        },
        scrollBehavior = scrollBehavior
    ) { padding ->
        ContentStateHost(state = state, onErrorAction = onErrorAction, contentPadding = padding) { details ->
            ProfileContent(details, onImageClick = { navigator.openImage(details.imageUrl) }, contentPadding = padding)
        }
    }
}

@Composable
fun ProfileContent(details: ProfileDetails, onImageClick: () -> Unit, contentPadding: PaddingValues) {
    LazyColumn(contentPadding = contentPadding.plus(PaddingValues(bottom = 16.dp))) {
        item(key = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProxerAsyncImage(
                    url = details.imageUrl,
                    contentDescription = null,
                    showErrorIcon = false,
                    modifier = Modifier
                        .width(120.dp)
                        .aspectRatio(0.75f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onImageClick)
                )

                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(details.name, style = MaterialTheme.typography.titleLarge)

                    details.facts.forEach { (title, content) -> InfoRow(title, content) }
                }
            }
        }

        if (details.names.isNotEmpty()) {
            item(key = "names") {
                SectionHeader(stringResource(R.string.profile_names))

                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.names.forEach { (title, content) -> InfoRow(title, content, isSelectable = true) }
                }
            }
        }

        details.descriptions.forEachIndexed { index, (title, text) ->
            item(key = "description_$index") {
                val tree = remember(text) { text.trim().toBBTree() }

                SectionHeader(title, Modifier.padding(top = 8.dp))

                BBCodeText(tree, Modifier.padding(horizontal = 16.dp), key = "profile_description_$index")
            }
        }

        details.groups.forEach { group ->
            item(key = "group_${group.title}") {
                SectionHeader(group.title, Modifier.padding(top = 8.dp))
            }

            items(group.items, key = { "${group.title}_${it.key}" }) { link ->
                ProfileListItem(link.title, link.subtitle, link.imageUrl, link.onClick)
            }
        }
    }
}

/**
 * A character, person or entry in a list.
 */
@Composable
fun ProfileListItem(title: String, subtitle: String?, imageUrl: HttpUrl?, onClick: () -> Unit) {
    Column {
        ListItem(
            headlineContent = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            supportingContent = subtitle?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            leadingContent = imageUrl?.let {
                {
                    ProxerAsyncImage(
                        url = it,
                        contentDescription = null,
                        showErrorIcon = false,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                }
            },
            modifier = Modifier.clickable(onClick = onClick)
        )

        HorizontalDivider(Modifier.padding(start = 80.dp))
    }
}
