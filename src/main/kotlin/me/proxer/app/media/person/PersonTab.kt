package me.proxer.app.media.person

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.info.ProfileListItem
import me.proxer.app.info.personTypeLabel
import me.proxer.app.info.personTypeOrder
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.library.entity.info.EntryPerson
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The persons involved in an entry, grouped by their role.
 */
@Composable
fun PersonTab(
    entryId: String,
    contentPadding: PaddingValues,
    viewModel: EntryPersonsViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current

    PersonContent(
        state = viewModel.collectContentState(),
        onErrorAction = rememberErrorActionHandler(viewModel::load),
        onPersonClick = { navigator.openPerson(it.id, it.name) },
        contentPadding = contentPadding
    )
}

@Composable
fun PersonContent(
    state: ContentState<List<EntryPerson>>,
    onErrorAction: (ErrorAction) -> Unit,
    onPersonClick: (EntryPerson) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_persons
    ) { persons ->
        val groups = remember(persons) {
            persons
                .groupBy { it.type }
                .toList()
                .sortedBy { (type, _) -> personTypeOrder(type) }
        }

        LazyColumn(contentPadding = contentPadding.plus(PaddingValues(bottom = 16.dp))) {
            groups.forEach { (type, group) ->
                item(key = "header_$type") {
                    SectionHeader(context.personTypeLabel(type))
                }

                items(group, key = { it.id + type }) { person ->
                    ProfileListItem(
                        title = person.name,
                        subtitle = null,
                        imageUrl = ProxerUrls.personImage(person.id),
                        onClick = { onPersonClick(person) }
                    )
                }
            }
        }
    }
}
