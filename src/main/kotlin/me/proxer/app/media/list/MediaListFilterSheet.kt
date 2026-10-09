package me.proxer.app.media.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.proxer.app.R
import me.proxer.app.media.LocalTag
import me.proxer.app.ui.components.ChipRow
import me.proxer.app.ui.components.LanguageFlag
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.util.extension.drawableRes
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toAppStringDescription
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Language

private const val COLLAPSED_TAG_COUNT = 40

/**
 * The extended search criteria of the anime and manga lists: language, genres, FSK and tags. Genres and tags can be
 * included or excluded by tapping them once or twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaListFilterSheet(
    filter: MediaListFilter,
    genres: List<LocalTag>?,
    tags: List<LocalTag>?,
    onApply: (MediaListFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(filter) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.fragment_media_list_language))

            ChipRow(Modifier.padding(horizontal = 16.dp)) {
                listOf(null, Language.GERMAN, Language.ENGLISH).forEach { language ->
                    FilterChip(
                        selected = draft.language == language,
                        onClick = { draft = draft.copy(language = language) },
                        label = {
                            Text(
                                stringResource(
                                    when (language) {
                                        Language.GERMAN -> R.string.language_german
                                        Language.ENGLISH -> R.string.language_english
                                        else -> R.string.fragment_media_list_all_languages
                                    }
                                )
                            )
                        },
                        leadingIcon = language?.let { { LanguageFlag(it) } }
                    )
                }
            }

            SectionHeader(stringResource(R.string.media_list_genres))

            TriStateTagChips(
                tags = genres,
                included = draft.genres,
                excluded = draft.excludedGenres,
                onChange = { included, excluded -> draft = draft.copy(genres = included, excludedGenres = excluded) },
                isSearchable = false
            )

            SectionHeader(stringResource(R.string.fragment_media_list_fsk))

            ChipRow(Modifier.padding(horizontal = 16.dp)) {
                FskConstraint.values().forEach { constraint ->
                    FskChip(
                        constraint = constraint,
                        isSelected = constraint in draft.fskConstraints,
                        onClick = {
                            draft = draft.copy(
                                fskConstraints = when (constraint in draft.fskConstraints) {
                                    true -> draft.fskConstraints - constraint
                                    false -> draft.fskConstraints + constraint
                                }
                            )
                        }
                    )
                }
            }

            SectionHeader(stringResource(R.string.media_list_tags))

            TriStateTagChips(
                tags = tags,
                included = draft.tags,
                excluded = draft.excludedTags,
                onChange = { included, excluded -> draft = draft.copy(tags = included, excludedTags = excluded) },
                isSearchable = true
            )

            SectionHeader(stringResource(R.string.media_list_options))

            CheckboxRow(
                text = stringResource(R.string.fragment_media_list_include_unrated_tags),
                isChecked = draft.includeUnratedTags,
                onCheckedChange = { draft = draft.copy(includeUnratedTags = it) }
            )

            CheckboxRow(
                text = stringResource(R.string.fragment_media_list_include_spoiler_tags),
                isChecked = draft.includeSpoilerTags,
                onCheckedChange = { draft = draft.copy(includeSpoilerTags = it) }
            )

            CheckboxRow(
                text = stringResource(R.string.fragment_media_list_hide_finished),
                isChecked = draft.hideFinished,
                onCheckedChange = { draft = draft.copy(hideFinished = it) }
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            OutlinedButton(onClick = { draft = draft.withoutExtendedCriteria() }) {
                Text(stringResource(R.string.action_reset))
            }

            Button(
                onClick = {
                    onApply(draft)

                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                }
            ) {
                Text(stringResource(R.string.fragment_media_list_search))
            }
        }
    }
}

@Composable
private fun TriStateTagChips(
    tags: List<LocalTag>?,
    included: List<LocalTag>,
    excluded: List<LocalTag>,
    onChange: (included: List<LocalTag>, excluded: List<LocalTag>) -> Unit,
    isSearchable: Boolean
) {
    if (tags == null) {
        Text(
            text = stringResource(R.string.media_list_tags_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    val includedIds = included.map { it.id }.toSet()
    val excludedIds = excluded.map { it.id }.toSet()

    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.media_list_tri_state_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isSearchable) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.media_list_tags_search_hint)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_symbol_search), contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Selected tags first, so they are visible even if the list is shortened.
        val matching = tags
            .filter { query.isBlank() || it.name.contains(query.trim(), ignoreCase = true) }
            .sortedByDescending { it.id in includedIds || it.id in excludedIds }

        val shown = if (isSearchable && query.isBlank()) matching.take(COLLAPSED_TAG_COUNT) else matching

        ChipRow {
            shown.forEach { tag ->
                val isIncluded = tag.id in includedIds
                val isExcluded = tag.id in excludedIds

                FilterChip(
                    selected = isIncluded || isExcluded,
                    onClick = {
                        when {
                            isIncluded -> onChange(included.filter { it.id != tag.id }, excluded + tag)
                            isExcluded -> onChange(included, excluded.filter { it.id != tag.id })
                            else -> onChange(included + tag, excluded)
                        }
                    },
                    label = { Text(tag.name) },
                    leadingIcon = when {
                        isIncluded -> {
                            { Icon(painterResource(R.drawable.ic_symbol_check), null, Modifier.size(18.dp)) }
                        }
                        isExcluded -> {
                            { Icon(painterResource(R.drawable.ic_symbol_remove), null, Modifier.size(18.dp)) }
                        }
                        else -> null
                    },
                    colors = when (isExcluded) {
                        true -> FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        false -> FilterChipDefaults.filterChipColors()
                    }
                )
            }
        }

        if (shown.size < matching.size) {
            Text(
                text = stringResource(R.string.media_list_tags_more, matching.size - shown.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FskChip(constraint: FskConstraint, isSelected: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(constraint.toAppString(context)) },
        leadingIcon = {
            Image(
                painter = painterResource(constraint.drawableRes),
                contentDescription = constraint.toAppStringDescription(context),
                modifier = Modifier.size(20.dp)
            )
        }
    )
}

@Composable
private fun CheckboxRow(text: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = isChecked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = isChecked, onCheckedChange = null)

        Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}
