package me.proxer.app.media.info

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.ui.components.COVER_ASPECT_RATIO
import me.proxer.app.ui.components.ChipRow
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.LanguageFlag
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.RatingStars
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.extension.drawableRes
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toAppStringDescription
import me.proxer.app.util.extension.toCategory
import me.proxer.app.util.extension.toEndAppString
import me.proxer.app.util.extension.toGeneralLanguage
import me.proxer.app.util.extension.toStartAppString
import me.proxer.app.util.extension.toTypeAppString
import me.proxer.library.entity.info.Entry
import me.proxer.library.entity.info.EntryTranslatorGroup
import me.proxer.library.entity.info.MediaUserInfo
import me.proxer.library.entity.list.IndustryCore
import me.proxer.library.enums.Category
import me.proxer.library.enums.Country
import me.proxer.library.enums.IndustryType
import me.proxer.library.enums.Language
import me.proxer.library.util.ProxerUrls

/**
 * The info tab of the media screen: the cover, rating and the actions of the user, followed by the description and
 * the details like genres, tags and the translator groups.
 */
@Composable
fun MediaInfoTab(viewModel: MediaInfoViewModel, state: ContentState<Entry>, contentPadding: PaddingValues) {
    val navigator = LocalAppNavigator.current
    val snackbarHostState = LocalSnackbarHostState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)
    val user by rememberCurrentUser()

    val userInfo by viewModel.userInfoData.observeAsState()
    val userInfoUpdate by viewModel.userInfoUpdateData.observeAsState()
    val userInfoUpdateError by viewModel.userInfoUpdateError.observeAsState()

    LaunchedEffect(userInfoUpdate) {
        if (userInfoUpdate != null) snackbarHostState?.showSnackbar(context.getString(R.string.fragment_set_user_info_success))
    }

    LaunchedEffect(userInfoUpdateError) {
        userInfoUpdateError?.let {
            snackbarHostState?.showSnackbar(context.getString(R.string.error_set_user_info, context.getString(it.message)))
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState?.showSnackbar(message) }
    }

    ContentStateHost(state = state, onErrorAction = onErrorAction, contentPadding = contentPadding) { entry ->
        MediaInfoContent(
            entry = entry,
            userInfo = userInfo,
            showUserActions = user != null,
            actions = MediaInfoActions(
                onCoverClick = { navigator.openImage(ProxerUrls.entryImage(entry.id)) },
                onNote = viewModel::note,
                onFavorite = viewModel::toggleFavorite,
                onFinish = viewModel::markAsFinished,
                onSubscribe = viewModel::toggleSubscription,
                onAdaptionClick = {
                    navigator.openMedia(entry.adaptionInfo.id, entry.adaptionInfo.name, entry.adaptionInfo.medium?.toCategory())
                },
                onTranslatorGroupClick = { navigator.openTranslatorGroup(it.id, it.name) },
                onIndustryClick = { navigator.openIndustry(it.id, it.name) },
                onMessage = ::showMessage
            ),
            contentPadding = contentPadding
        )
    }
}

/**
 * The callbacks of [MediaInfoContent].
 */
class MediaInfoActions(
    val onCoverClick: () -> Unit = {},
    val onNote: () -> Unit = {},
    val onFavorite: () -> Unit = {},
    val onFinish: () -> Unit = {},
    val onSubscribe: () -> Unit = {},
    val onAdaptionClick: () -> Unit = {},
    val onTranslatorGroupClick: (EntryTranslatorGroup) -> Unit = {},
    val onIndustryClick: (IndustryCore) -> Unit = {},
    val onMessage: (String) -> Unit = {}
)

@Composable
fun MediaInfoContent(
    entry: Entry,
    userInfo: MediaUserInfo?,
    showUserActions: Boolean,
    actions: MediaInfoActions,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    LazyColumn(contentPadding = contentPadding.plus(PaddingValues(bottom = 16.dp))) {
        item(key = "header") {
            MediaHeader(entry, onCoverClick = actions.onCoverClick)
        }

        if (showUserActions) {
            item(key = "actions") {
                UserActions(
                    userInfo = userInfo,
                    onNote = actions.onNote,
                    onFavorite = actions.onFavorite,
                    onFinish = actions.onFinish,
                    onSubscribe = actions.onSubscribe
                )
            }
        }

        if (entry.description.isNotBlank()) {
            item(key = "description") {
                ExpandableDescription(entry.description.trim())
            }
        }

        item(key = "general") {
            GeneralInfo(entry, onAdaptionClick = actions.onAdaptionClick)
        }

        if (entry.genres.isNotEmpty()) {
            item(key = "genres") {
                SectionHeader(stringResource(R.string.fragment_media_info_genres))

                ChipRow(Modifier.padding(horizontal = 16.dp)) {
                    entry.genres.forEach { genre ->
                        AssistChip(onClick = { actions.onMessage(genre.description) }, label = { Text(genre.name) })
                    }
                }
            }
        }

        if (entry.tags.isNotEmpty()) {
            item(key = "tags") {
                Tags(entry, onTagClick = actions.onMessage)
            }
        }

        if (entry.fskConstraints.isNotEmpty()) {
            item(key = "fsk") {
                SectionHeader(stringResource(R.string.fragment_media_info_fsk_constraints))

                ChipRow(Modifier.padding(horizontal = 16.dp)) {
                    entry.fskConstraints.forEach { constraint ->
                        AssistChip(
                            onClick = { actions.onMessage(constraint.toAppStringDescription(context)) },
                            label = { Text(constraint.toAppString(context)) },
                            leadingIcon = {
                                Image(
                                    painterResource(constraint.drawableRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        if (entry.translatorGroups.isNotEmpty()) {
            item(key = "translatorGroups") {
                SectionHeader(stringResource(R.string.fragment_media_info_translator_groups))

                ChipRow(Modifier.padding(horizontal = 16.dp)) {
                    entry.translatorGroups.forEach { group ->
                        AssistChip(
                            onClick = { actions.onTranslatorGroupClick(group) },
                            label = { Text(group.name) },
                            leadingIcon = group.country.toLanguage()?.let { { LanguageFlag(it) } }
                        )
                    }
                }
            }
        }

        if (entry.industries.isNotEmpty()) {
            item(key = "industries") {
                SectionHeader(stringResource(R.string.fragment_media_info_industries))

                ChipRow(Modifier.padding(horizontal = 16.dp)) {
                    entry.industries.forEach { industry ->
                        AssistChip(
                            onClick = { actions.onIndustryClick(industry) },
                            label = {
                                Text(
                                    when (industry.type) {
                                        IndustryType.UNKNOWN -> industry.name
                                        else -> "${industry.name} (${industry.type.toAppString(context)})"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaHeader(entry: Entry, onCoverClick: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProxerAsyncImage(
            url = ProxerUrls.entryImage(entry.id),
            contentDescription = null,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(COVER_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onCoverClick)
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(entry.name, style = MaterialTheme.typography.titleLarge)

            Text(
                text = entry.medium.toAppString(context) + " · " + pluralStringResource(
                    if (entry.category == Category.ANIME) R.plurals.media_episode_count else R.plurals.media_chapter_count,
                    entry.episodeAmount,
                    entry.episodeAmount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = entry.state.toAppString(context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (entry.rating > 0) {
                Spacer(Modifier.height(4.dp))

                RatingStars(entry.rating / 2f, starSize = 18.dp)

                Text(
                    text = pluralStringResource(
                        R.plurals.fragment_media_info_rate_count,
                        entry.ratingAmount,
                        entry.rating,
                        entry.ratingAmount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                entry.languages.map { it.toGeneralLanguage() }.distinct().filter { it != Language.OTHER }.forEach {
                    LanguageFlag(it)
                }
            }
        }
    }
}

@Composable
private fun UserActions(
    userInfo: MediaUserInfo?,
    onNote: () -> Unit,
    onFavorite: () -> Unit,
    onFinish: () -> Unit,
    onSubscribe: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        UserAction(R.drawable.ic_symbol_schedule, R.string.fragment_media_info_note, userInfo?.isNoted == true, onNote)
        UserAction(R.drawable.ic_symbol_star, R.string.fragment_media_info_favor, userInfo?.isTopTen == true, onFavorite)
        UserAction(R.drawable.ic_symbol_check_circle, R.string.fragment_media_info_finish, userInfo?.isFinished == true, onFinish)
        UserAction(
            R.drawable.ic_symbol_notifications,
            R.string.fragment_media_info_subscribe,
            userInfo?.isSubscribed == true,
            onSubscribe
        )
    }

    HorizontalDivider(Modifier.padding(top = 8.dp))
}

@Composable
private fun UserAction(@DrawableRes icon: Int, @StringRes label: Int, isChecked: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
        FilledTonalIconToggleButton(checked = isChecked, onCheckedChange = { onClick() }) {
            Icon(painterResource(icon), contentDescription = stringResource(label))
        }

        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExpandableDescription(description: String) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var isOverflowing by rememberSaveable { mutableStateOf(false) }

    SectionHeader(stringResource(R.string.fragment_media_info_description), Modifier.padding(top = 8.dp))

    Column(Modifier.padding(horizontal = 16.dp)) {
        SelectionContainer {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 6,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!isExpanded) isOverflowing = it.hasVisualOverflow }
            )
        }

        if (isOverflowing || isExpanded) {
            TextButton(onClick = { isExpanded = !isExpanded }) {
                Text(
                    stringResource(
                        if (isExpanded) R.string.media_info_show_less else R.string.fragment_media_info_show_all
                    )
                )
            }
        }
    }
}

@Composable
private fun GeneralInfo(entry: Entry, onAdaptionClick: () -> Unit) {
    val context = LocalContext.current

    SectionHeader(stringResource(R.string.fragment_media_info_general))

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entry.synonyms.forEach { synonym ->
            InfoRow(synonym.toTypeAppString(context), synonym.name, isSelectable = true)
        }

        if (entry.seasons.isNotEmpty()) {
            val seasons = listOfNotNull(
                entry.seasons.getOrNull(0)?.toStartAppString(context),
                entry.seasons.getOrNull(1)?.toEndAppString(context)
            )

            InfoRow(stringResource(R.string.fragment_media_info_season_title), seasons.joinToString("\n"))
        }

        InfoRow(stringResource(R.string.fragment_media_info_status_title), entry.state.toAppString(context))
        InfoRow(stringResource(R.string.fragment_media_info_license_title), entry.license.toAppString(context))

        if (entry.adaptionInfo.id != "0") {
            val adaption = entry.adaptionInfo

            InfoRow(
                title = stringResource(R.string.fragment_media_info_adaption_title),
                content = "${adaption.name} (${adaption.medium?.toAppString(context)})",
                onClick = onAdaptionClick
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, content: String, isSelectable: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp)
        )

        val contentModifier = Modifier
            .weight(1f)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }

        val contentText: @Composable () -> Unit = {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = contentModifier
            )
        }

        if (isSelectable) SelectionContainer(contentModifier) { contentText() } else contentText()
    }
}

@Composable
private fun Tags(entry: Entry, onTagClick: (String) -> Unit) {
    var showUnrated by rememberSaveable { mutableStateOf(false) }
    var showSpoilers by rememberSaveable { mutableStateOf(false) }

    val tags = entry.tags.filter { tag ->
        (tag.isRated || showUnrated) && (!tag.isSpoiler || showSpoilers)
    }

    SectionHeader(stringResource(R.string.fragment_media_info_tags))

    ChipRow(Modifier.padding(horizontal = 16.dp)) {
        FilterChip(
            selected = showUnrated,
            onClick = { showUnrated = !showUnrated },
            label = { Text(stringResource(R.string.media_info_unrated_tags)) }
        )

        FilterChip(
            selected = showSpoilers,
            onClick = { showSpoilers = !showSpoilers },
            label = { Text(stringResource(R.string.media_info_spoiler_tags)) }
        )

        tags.forEach { tag ->
            AssistChip(onClick = { onTagClick(tag.description) }, label = { Text(tag.name) })
        }
    }
}

private fun Country.toLanguage() = when (this) {
    Country.GERMANY -> Language.GERMAN
    Country.ENGLAND, Country.UNITED_STATES -> Language.ENGLISH
    else -> null
}
