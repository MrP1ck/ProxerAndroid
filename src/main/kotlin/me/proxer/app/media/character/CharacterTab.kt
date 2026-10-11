package me.proxer.app.media.character

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import me.proxer.app.R
import me.proxer.app.info.ProfileListItem
import me.proxer.app.info.characterGroupLabel
import me.proxer.app.info.characterTypeOrder
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.library.entity.info.EntryCharacter
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The characters of an entry, grouped by their importance.
 */
@Composable
fun CharacterTab(
    entryId: String,
    contentPadding: PaddingValues,
    viewModel: EntryCharactersViewModel = koinViewModel(parameters = { parametersOf(entryId) })
) {
    val navigator = LocalAppNavigator.current

    CharacterContent(
        state = viewModel.collectContentState(),
        onErrorAction = rememberErrorActionHandler(viewModel::load),
        onCharacterClick = { navigator.openCharacter(it.id, it.name) },
        contentPadding = contentPadding
    )
}

@Composable
fun CharacterContent(
    state: ContentState<List<EntryCharacter>>,
    onErrorAction: (ErrorAction) -> Unit,
    onCharacterClick: (EntryCharacter) -> Unit,
    contentPadding: PaddingValues
) {
    val context = LocalContext.current

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_characters
    ) { characters ->
        val groups = remember(characters) {
            characters
                .groupBy { it.type }
                .toList()
                .sortedBy { (type, _) -> characterTypeOrder(type) }
        }

        LazyColumn(contentPadding = contentPadding.plus(PaddingValues(bottom = 16.dp))) {
            groups.forEach { (type, group) ->
                item(key = "header_$type") {
                    SectionHeader(context.characterGroupLabel(type))
                }

                items(group, key = { it.id }) { character ->
                    ProfileListItem(
                        title = character.name,
                        subtitle = null,
                        imageUrl = ProxerUrls.characterImage(character.id),
                        onClick = { onCharacterClick(character) }
                    )
                }
            }
        }
    }
}
