package me.proxer.app.chat.prv.conference.info

import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.chat.UserListItem
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.Utils
import me.proxer.app.util.extension.getSafeParcelableExtra
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toLocalDateTimeBP
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The info of a group conference: when it was created and its participants.
 *
 * @author Ruben Gees
 */
class ConferenceInfoActivity : ComposeActivity() {

    companion object {
        private const val CONFERENCE_EXTRA = "conference"

        fun navigateTo(context: Activity, conference: LocalConference) {
            context.startActivity<ConferenceInfoActivity>(CONFERENCE_EXTRA to conference)
        }
    }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val conference = remember { intent.getSafeParcelableExtra<LocalConference>(CONFERENCE_EXTRA) }
        val viewModel = koinViewModel<ConferenceInfoViewModel> { parametersOf(conference.id.toString()) }
        val state = viewModel.collectContentState()
        val onErrorAction = rememberErrorActionHandler(viewModel::load)

        ProxerScaffold(title = conference.topic, onNavigateUp = navigator::navigateUp) { padding ->
            ContentStateHost(state, onErrorAction, contentPadding = padding) { info ->
                val creation = remember(info.firstMessageTime) { info.firstMessageTime.toLocalDateTimeBP() }

                LazyColumn(contentPadding = padding) {
                    item(key = "info") {
                        SectionHeader(stringResource(R.string.fragment_conference_info_info_title))

                        Text(
                            text = stringResource(
                                R.string.fragment_conference_info_time,
                                Utils.dateFormatter.format(creation),
                                Utils.timeFormatter.format(creation)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    item(key = "participants") {
                        SectionHeader(
                            stringResource(R.string.fragment_conference_info_participants_title),
                            Modifier.padding(top = 8.dp)
                        )
                    }

                    items(info.participants, key = { it.id }) { participant ->
                        UserListItem(
                            username = participant.username,
                            image = participant.image,
                            status = participant.status,
                            badge = R.drawable.ic_symbol_star_filled.takeIf { participant.id == info.leaderId },
                            onClick = { navigator.openProfile(participant.id, participant.username, participant.image) }
                        )
                    }
                }
            }
        }
    }
}
