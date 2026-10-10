package me.proxer.app.profile.info

import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.profile.ProfileViewModel
import me.proxer.app.profile.ProfileViewModel.UserInfoWrapper
import me.proxer.app.ui.components.ChipRow
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.rememberLinkifiedText
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.library.util.ProxerUrls
import okhttp3.HttpUrl
import java.util.Locale

private const val RANK_FORUM_ID = "207664"
private const val RANK_FORUM_CATEGORY_ID = "79"
private const val RANK_FORUM_TOPIC = "Rangpunkte und Ränge"
private const val MINUTES_PER_EPISODE = 20

/**
 * The avatar, status, points and rank of a user, and the episode counter on the own profile.
 */
@Composable
fun ProfileInfoTab(
    viewModel: ProfileViewModel,
    state: ContentState<UserInfoWrapper>,
    image: String?,
    contentPadding: PaddingValues
) {
    val navigator = LocalAppNavigator.current
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    ContentStateHost(state = state, onErrorAction = onErrorAction, contentPadding = contentPadding) { data ->
        ProfileInfoContent(
            data = data,
            image = image,
            onAvatarClick = { if (!image.isNullOrBlank()) navigator.openImage(ProxerUrls.userImage(image)) },
            onRankClick = { navigator.openTopic(RANK_FORUM_ID, RANK_FORUM_CATEGORY_ID, RANK_FORUM_TOPIC) },
            onLinkClick = navigator::showPage,
            contentPadding = contentPadding
        )
    }
}

@Composable
fun ProfileInfoContent(
    data: UserInfoWrapper,
    image: String?,
    onAvatarClick: () -> Unit,
    onRankClick: () -> Unit,
    onLinkClick: (HttpUrl) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current
    val (info, watchedEpisodes) = data
    val totalPoints = info.animePoints + info.mangaPoints + info.uploadPoints + info.forumPoints + info.infoPoints +
        info.miscPoints

    LazyColumn(contentPadding = contentPadding.plus(PaddingValues(16.dp))) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (image.isNullOrBlank()) {
                        Icon(
                            painterResource(R.drawable.ic_symbol_person),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        ProxerAsyncImage(
                            ProxerUrls.userImage(image),
                            null,
                            Modifier.size(112.dp),
                            showErrorIcon = false
                        )
                    }
                }

                Text(
                    info.username,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )

                ChipRow(Modifier.padding(top = 8.dp)) {
                    AssistChip(onClick = onRankClick, label = { Text(rankName(totalPoints)) })

                    if (info.isTeamMember) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.profile_team)) }
                        )
                    }
                    if (info.isDonator) {
                        AssistChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.profile_donator)) }
                        )
                    }
                }
            }
        }

        if (info.status.isNotBlank()) {
            item(key = "status") {
                SectionHeader(stringResource(R.string.fragment_profile_status), Modifier.padding(top = 8.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            rememberLinkifiedText(info.status, onLinkClick),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            info.lastStatusChange.distanceInWordsToNow(context),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item(key = "points") {
            SectionHeader(stringResource(R.string.fragment_profile_points), Modifier.padding(top = 8.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatRow(R.string.fragment_profile_points_anime, info.animePoints.toString())
                    StatRow(R.string.fragment_profile_points_manga, info.mangaPoints.toString())
                    StatRow(R.string.fragment_profile_points_uploads, info.uploadPoints.toString())
                    StatRow(R.string.fragment_profile_points_forum, info.forumPoints.toString())
                    StatRow(R.string.fragment_profile_points_info, info.infoPoints.toString())
                    StatRow(R.string.fragment_profile_points_miscellaneous, info.miscPoints.toString())
                    StatRow(R.string.fragment_profile_points_total, totalPoints.toString(), isEmphasized = true)
                }
            }
        }

        if (watchedEpisodes != null) {
            item(key = "episodes") {
                val minutes = watchedEpisodes * MINUTES_PER_EPISODE
                val hours = minutes / 60f
                val days = hours / 24f

                SectionHeader(stringResource(R.string.fragment_profile_episode_counter), Modifier.padding(top = 8.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatRow(R.string.fragment_profile_epsiode_counter_episodes, watchedEpisodes.toString())
                        StatRow(R.string.fragment_profile_epsiode_counter_minutes, minutes.toString())
                        StatRow(
                            R.string.fragment_profile_epsiode_counter_hours,
                            String.format(Locale.GERMANY, "%.1f", hours)
                        )
                        StatRow(
                            R.string.fragment_profile_epsiode_counter_days,
                            String.format(Locale.GERMANY, "%.1f", days)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(@StringRes title: Int, value: String, isEmphasized: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            stringResource(title),
            style = if (isEmphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = if (isEmphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun rankName(points: Int) = stringResource(
    when {
        points < 10 -> R.string.rank_10
        points < 100 -> R.string.rank_100
        points < 200 -> R.string.rank_200
        points < 500 -> R.string.rank_500
        points < 700 -> R.string.rank_700
        points < 1_000 -> R.string.rank_1000
        points < 1_500 -> R.string.rank_1500
        points < 2_000 -> R.string.rank_2000
        points < 3_000 -> R.string.rank_3000
        points < 4_000 -> R.string.rank_4000
        points < 6_000 -> R.string.rank_6000
        points < 8_000 -> R.string.rank_8000
        points < 10_000 -> R.string.rank_10000
        points < 11_000 -> R.string.rank_11000
        points < 12_000 -> R.string.rank_12000
        points < 14_000 -> R.string.rank_14000
        points < 16_000 -> R.string.rank_16000
        points < 18_000 -> R.string.rank_18000
        points < 20_000 -> R.string.rank_20000
        else -> R.string.rank_kami_sama
    }
)
