package me.proxer.app.anime

import android.app.Activity
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import me.proxer.app.R
import me.proxer.app.anime.resolver.ProxerStreamResolver
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ErrorState
import me.proxer.app.ui.components.LoadingState
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.app.ui.components.MediaControlTexts
import me.proxer.app.ui.components.MediaControls
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.TopAppBarTitle
import me.proxer.app.ui.components.UpButton
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.rememberLinkifiedText
import me.proxer.app.ui.components.showErrorSnackbar
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.shell.rememberCurrentUser
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.ErrorUtils.ErrorAction.Companion.ACTION_MESSAGE_HIDE
import me.proxer.app.util.Utils
import me.proxer.app.util.compat.isConnectedToCellular
import me.proxer.app.util.data.PreferenceHelper
import me.proxer.app.util.data.StorageHelper
import me.proxer.app.util.extension.androidUri
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.library.entity.info.EntryCore
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls
import me.proxer.library.util.ProxerUtils
import okhttp3.HttpUrl
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit

/**
 * The streams of an episode of an anime.
 *
 * @author Ruben Gees
 */
class AnimeActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val EPISODE_EXTRA = "episode"
        private const val LANGUAGE_EXTRA = "language"
        private const val NAME_EXTRA = "name"
        private const val EPISODE_AMOUNT_EXTRA = "episode_amount"

        fun navigateTo(
            context: Activity,
            id: String,
            episode: Int,
            language: AnimeLanguage,
            name: String? = null,
            episodeAmount: Int? = null
        ) {
            context.startActivity<AnimeActivity>(
                ID_EXTRA to id,
                EPISODE_EXTRA to episode,
                LANGUAGE_EXTRA to language,
                NAME_EXTRA to name,
                EPISODE_AMOUNT_EXTRA to episodeAmount
            )
        }
    }

    private val id: String
        get() = when (intent.hasExtra(ID_EXTRA)) {
            true -> intent.getSafeStringExtra(ID_EXTRA)
            false -> intent?.data?.pathSegments?.getOrNull(1) ?: "-1"
        }

    private val initialEpisode: Int
        get() = when (intent.hasExtra(EPISODE_EXTRA)) {
            true -> intent.getIntExtra(EPISODE_EXTRA, 1)
            false -> intent?.data?.pathSegments?.getOrNull(2)?.toIntOrNull() ?: 1
        }

    private val language: AnimeLanguage
        get() = when (intent.hasExtra(LANGUAGE_EXTRA)) {
            true -> intent.getSerializableExtra(LANGUAGE_EXTRA) as AnimeLanguage
            false ->
                intent?.data?.pathSegments?.getOrNull(3)?.let { ProxerUtils.toApiEnum<AnimeLanguage>(it) }
                    ?: AnimeLanguage.ENGLISH_SUB
        }

    private val viewModel by viewModel<AnimeViewModel> { parametersOf(id, language, initialEpisode) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportFragmentManager.setFragmentResultListener(NoWifiDialog.STREAM_ID_RESULT, this) { _, bundle ->
            viewModel.data.value?.streams
                ?.find { it.id == bundle.getString(NoWifiDialog.STREAM_ID_RESULT) }
                ?.let { viewModel.resolve(it) }
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
        val resolutionResult by viewModel.resolutionResult.observeAsState()
        val resolutionError by viewModel.resolutionError.observeAsState()
        val userStateData by viewModel.userStateData.observeAsState()
        val userStateError by viewModel.userStateError.observeAsState()

        var episode by rememberSaveable { mutableIntStateOf(viewModel.episode) }

        // The entry is also known if loading the streams failed, e.g. because there are none for this episode.
        val entry = state.data?.let { it.name to it.episodeAmount }
            ?: (state.error?.data?.get(ErrorUtils.ENTRY_DATA_KEY) as? EntryCore)?.let { it.name to it.episodeAmount }
        val name = entry?.first ?: intent.getStringExtra(NAME_EXTRA)
        val episodeAmount = entry?.second ?: intent.getIntExtra(EPISODE_AMOUNT_EXTRA, Int.MAX_VALUE)

        fun switchEpisode(newEpisode: Int) {
            if (preferenceHelper.areBookmarksAutomatic && newEpisode > episode && user != null) {
                viewModel.bookmark(newEpisode)
            }

            episode = newEpisode
            viewModel.episode = newEpisode
            intent.putExtra(EPISODE_EXTRA, newEpisode)
        }

        LaunchedEffect(resolutionResult) {
            when (val result = resolutionResult) {
                is StreamResolutionResult.Video -> result.play(
                    context,
                    id,
                    name,
                    episode,
                    language,
                    ProxerUrls.entryImage(id).androidUri(),
                    true
                )
                is StreamResolutionResult.Link -> result.show(this@AnimeActivity)
                is StreamResolutionResult.App -> result.navigate(context)
                else -> Unit
            }
        }

        ProxerScaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(Modifier.clickable(enabled = name != null) { navigator.openMedia(id, name, Category.ANIME) }) {
                            val episodeTitle = Category.ANIME.toEpisodeAppString(context, episode)

                            if (name == null) TopAppBarTitle(episodeTitle) else TopAppBarTitle(name, episodeTitle)
                        }
                    },
                    navigationIcon = { UpButton(navigator::navigateUp) },
                    actions = {
                        if (name != null) {
                            IconButton(
                                onClick = {
                                    navigator.share(
                                        getString(R.string.share_anime, episode, name, ProxerUrls.animeWeb(id, episode, language))
                                    )
                                }
                            ) {
                                Icon(painterResource(R.drawable.ic_symbol_share), contentDescription = stringResource(R.string.action_share))
                            }
                        }
                    }
                )
            }
        ) { padding ->
            val snackbarHostState = LocalSnackbarHostState.current

            LaunchedEffect(resolutionError) {
                when (val error = resolutionError) {
                    null -> Unit
                    is AppRequiredErrorAction -> error.showDialog(this@AnimeActivity)
                    else -> snackbarHostState?.showErrorSnackbar(context, error, onErrorAction)
                }
            }

            LaunchedEffect(userStateData) {
                if (userStateData != null) {
                    snackbarHostState?.showSnackbar(context.getString(R.string.fragment_set_user_info_success))
                }
            }

            LaunchedEffect(userStateError) {
                userStateError?.let {
                    snackbarHostState?.showErrorSnackbar(
                        context = context,
                        error = it,
                        onAction = onErrorAction,
                        message = context.getString(R.string.error_set_user_info, context.getString(it.message))
                    )
                }
            }

            AnimeContent(
                state = state,
                hasEntry = entry != null,
                episode = episode,
                episodeAmount = episodeAmount,
                isLoggedIn = user != null,
                contentPadding = padding,
                onErrorAction = onErrorAction,
                onSwitchEpisode = ::switchEpisode,
                onBookmark = viewModel::bookmark,
                onFinish = viewModel::markAsFinished,
                actions = StreamActions(
                    onPlay = { stream ->
                        val connectivityManager = getSystemService<ConnectivityManager>()

                        if (connectivityManager?.isConnectedToCellular == true && preferenceHelper.shouldCheckCellular) {
                            NoWifiDialog.show(this@AnimeActivity, stream.id)
                        } else {
                            viewModel.resolve(stream)
                        }
                    },
                    onLogin = navigator::showLogin,
                    onUploaderClick = { navigator.openProfile(it.uploaderId, it.uploaderName) },
                    onTranslatorGroupClick = { stream ->
                        stream.translatorGroupId?.let { navigator.openTranslatorGroup(it, stream.translatorGroupName) }
                    },
                    onLinkClick = { navigator.showPage(it, skipCheck = true) },
                    shouldShowAdAlert = { stream ->
                        ProxerStreamResolver.supports(stream.hosterName) &&
                            storageHelper.lastAdAlertDate.plus(14, ChronoUnit.DAYS).isBefore(Instant.now()) &&
                            storageHelper.profileSettings.adInterval <= 0
                    },
                    onDismissAdAlert = { storageHelper.lastAdAlertDate = Instant.now() },
                    onSetAdInterval = navigator::openProfileSettings
                )
            )
        }
    }
}

/**
 * The actions of the streams in [AnimeContent].
 */
internal class StreamActions(
    val onPlay: (AnimeStream) -> Unit = {},
    val onLogin: () -> Unit = {},
    val onUploaderClick: (AnimeStream) -> Unit = {},
    val onTranslatorGroupClick: (AnimeStream) -> Unit = {},
    val onLinkClick: (HttpUrl) -> Unit = {},
    val shouldShowAdAlert: (AnimeStream) -> Boolean = { false },
    val onDismissAdAlert: () -> Unit = {},
    val onSetAdInterval: () -> Unit = {}
)

@Composable
internal fun AnimeContent(
    state: ContentState<AnimeStreamInfo>,
    hasEntry: Boolean,
    episode: Int,
    episodeAmount: Int,
    isLoggedIn: Boolean,
    contentPadding: PaddingValues,
    onErrorAction: (ErrorAction) -> Unit,
    onSwitchEpisode: (Int) -> Unit,
    onBookmark: (Int) -> Unit,
    onFinish: () -> Unit,
    actions: StreamActions
) {
    var expandedStreamId by rememberSaveable(episode) { mutableStateOf<String?>(null) }

    when {
        !hasEntry && state.error != null -> ErrorState(state.error, onErrorAction, Modifier.padding(contentPadding))
        !hasEntry -> LoadingState(Modifier.padding(contentPadding))
        else -> Column(
            Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding())
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp) + PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "controls") {
                    MediaControls(
                        current = episode,
                        amount = episodeAmount,
                        texts = MediaControlTexts(
                            previous = stringResource(R.string.fragment_anime_previous_episode),
                            next = stringResource(R.string.fragment_anime_next_episode),
                            bookmarkThis = stringResource(R.string.fragment_anime_bookmark_this_episode),
                            bookmarkNext = stringResource(R.string.fragment_anime_bookmark_next_episode)
                        ),
                        onSwitch = onSwitchEpisode,
                        onBookmark = onBookmark,
                        onFinish = onFinish
                    )
                }

                val streams = state.data?.streams

                when {
                    state.error != null -> item(key = "error") { ErrorState(state.error, onErrorAction) }
                    streams == null -> item(key = "loading") { LoadingState() }
                    streams.isEmpty() -> item(key = "empty") {
                        ErrorState(ErrorAction(R.string.error_no_data_anime, ACTION_MESSAGE_HIDE), onErrorAction)
                    }
                    else -> items(streams, key = { it.id }) { stream ->
                        when (val result = stream.resolutionResult) {
                            is StreamResolutionResult.Message -> StreamMessage(result.message, actions.onLinkClick)
                            else -> StreamCard(
                                stream = stream,
                                isExpanded = expandedStreamId == stream.id,
                                isLoggedIn = isLoggedIn,
                                onToggle = { expandedStreamId = if (expandedStreamId == stream.id) null else stream.id },
                                actions = actions
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamCard(
    stream: AnimeStream,
    isExpanded: Boolean,
    isLoggedIn: Boolean,
    onToggle: () -> Unit,
    actions: StreamActions
) {
    val isLoginRequired = !stream.isPublic && !isLoggedIn

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProxerAsyncImage(
                url = ProxerUrls.hosterImage(stream.image),
                contentDescription = null,
                showErrorIcon = false,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.width(16.dp))

            Text(
                text = stream.hosterName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onToggle) {
                Icon(
                    painter = painterResource(R.drawable.ic_symbol_expand_more),
                    contentDescription = null,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f)
                )
            }
        }

        AnimatedVisibility(isExpanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreamInfoRow(stringResource(R.string.view_media_control_uploader), stream.uploaderName) {
                    actions.onUploaderClick(stream)
                }

                StreamInfoRow(
                    label = stringResource(R.string.view_media_control_translator_group),
                    value = stream.translatorGroupName ?: stringResource(R.string.fragment_anime_empty_subgroup),
                    onClick = if (stream.translatorGroupId != null && stream.translatorGroupName != null) {
                        { actions.onTranslatorGroupClick(stream) }
                    } else {
                        null
                    }
                )

                StreamInfoRow(
                    stringResource(R.string.view_media_control_date),
                    stream.date.toLocalDateTime().format(Utils.dateFormatter),
                    null
                )

                StreamButtons(stream, isLoginRequired, actions)
            }
        }
    }
}

@Composable
private fun StreamButtons(stream: AnimeStream, isLoginRequired: Boolean, actions: StreamActions) {
    var adAlertDismissals by remember { mutableIntStateOf(0) }
    val shouldShowAdAlert = remember(stream, adAlertDismissals) { actions.shouldShowAdAlert(stream) }

    when {
        shouldShowAdAlert -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.fragment_anime_ad_alert).trim(), style = MaterialTheme.typography.bodyMedium)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                TextButton(
                    onClick = {
                        actions.onDismissAdAlert()
                        adAlertDismissals++
                    }
                ) {
                    Text(stringResource(R.string.fragment_anime_ad_alert_dismiss))
                }

                FilledTonalButton(
                    onClick = {
                        actions.onDismissAdAlert()
                        adAlertDismissals++

                        actions.onSetAdInterval()
                    }
                ) {
                    Text(stringResource(R.string.fragment_anime_ad_alert_set_interval))
                }
            }
        }
        !stream.isSupported -> StreamInfoText(stringResource(R.string.error_unsupported_hoster), null)
        else -> {
            when {
                isLoginRequired -> StreamInfoText(
                    stringResource(R.string.fragment_anime_stream_login_required_warning).trim(),
                    R.drawable.ic_symbol_lock
                )
                stream.isOfficial -> StreamInfoText(
                    stringResource(R.string.fragment_anime_stream_official_info).trim(),
                    R.drawable.ic_symbol_info
                )
            }

            Button(
                onClick = { if (isLoginRequired) actions.onLogin() else actions.onPlay(stream) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(if (isLoginRequired) R.drawable.ic_symbol_login else R.drawable.ic_symbol_play_arrow),
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(if (isLoginRequired) R.string.error_action_login else R.string.fragment_anime_stream_play))
            }
        }
    }
}

@Composable
private fun StreamInfoRow(label: String, value: String, onClick: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 16.dp)
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier
        )
    }
}

@Composable
private fun StreamInfoText(text: String, icon: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))

            Spacer(Modifier.width(12.dp))
        }

        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StreamMessage(message: CharSequence, onLinkClick: (HttpUrl) -> Unit) {
    val text = rememberLinkifiedText(message.toString(), onLinkClick)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}
