package me.proxer.app.info.character

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.discover.DirectoryActivity
import me.proxer.app.discover.DirectoryKind
import me.proxer.app.info.ProfileDetails
import me.proxer.app.info.ProfileGroup
import me.proxer.app.info.ProfileLink
import me.proxer.app.info.ProfileScreen
import me.proxer.app.info.birthdayLabel
import me.proxer.app.info.characterTypeLabel
import me.proxer.app.info.genderLabel
import me.proxer.app.info.languageLabel
import me.proxer.app.info.subjectLabel
import me.proxer.app.info.takeIfKnown
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.map
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.AppNavigator
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.startActivity
import me.proxer.library.entity.info.Character
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Shows a character with the persons voicing it and the entries it appears in. Also handles the deep links to
 * characters.
 */
class CharacterActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity<CharacterActivity>(
                ID_EXTRA to id,
                NAME_EXTRA to name
            )
        }
    }

    private val id: String?
        get() = intent.getStringExtra(ID_EXTRA) ?: intent.data?.getQueryParameter("id")

    @Composable
    override fun Content() {
        val id = id

        // Links to the list of the website have no id.
        if (id == null) {
            LaunchedEffect(Unit) {
                DirectoryActivity.navigateTo(this@CharacterActivity, DirectoryKind.CHARACTERS)
                finish()
            }

            return
        }

        val context = LocalContext.current
        val navigator = LocalAppNavigator.current
        val viewModel = koinViewModel<CharacterViewModel> { parametersOf(id) }
        val state = viewModel.collectContentState()

        ProfileScreen(
            initialName = intent.getStringExtra(NAME_EXTRA),
            shareText = R.string.share_character,
            shareUrl = ProxerUrls.characterWeb(id),
            state = state.map { it.toDetails(context, navigator) },
            onErrorAction = rememberErrorActionHandler(viewModel::load)
        )
    }
}

private fun Character.toDetails(context: Context, navigator: AppNavigator) = ProfileDetails(
    name = name,
    imageUrl = ProxerUrls.characterImage(id),
    facts = listOfNotNull(
        context.genderLabel(gender)?.let { context.getString(R.string.profile_gender) to it },
        context.birthdayLabel(birthday)?.let { context.getString(R.string.profile_birthday) to it },
        bloodType.takeIfKnown()?.let { context.getString(R.string.profile_blood_type) to it },
        height.takeIfKnown()?.let {
            context.getString(R.string.profile_height) to context.getString(R.string.profile_centimeters, it)
        },
        weight.takeIfKnown()?.let {
            context.getString(R.string.profile_weight) to context.getString(R.string.profile_kilograms, it)
        }
    ),
    names = names.map { name ->
        val title = languageLabel(name.language) ?: context.getString(R.string.profile_name_default)
        val alternative = name.alternative.takeIfKnown()

        title to when (alternative) {
            null -> name.name
            else -> context.getString(R.string.profile_name_with_alternative, name.name, alternative)
        }
    },
    descriptions = descriptions
        .filter { it.text.isNotBlank() }
        .map { context.subjectLabel(it.subject) to it.text },
    groups = listOf(
        ProfileGroup(
            title = context.getString(R.string.profile_voiced_by),
            items = persons.mapIndexed { index, person ->
                ProfileLink(
                    key = "${person.id}_$index",
                    title = person.name,
                    subtitle = languageLabel(person.language),
                    imageUrl = ProxerUrls.personImage(person.id),
                    onClick = { navigator.openPerson(person.id, person.name) }
                )
            }
        ),
        ProfileGroup(
            title = context.getString(R.string.profile_appears_in),
            items = entries.mapIndexed { index, entry ->
                ProfileLink(
                    key = "${entry.id}_$index",
                    title = entry.name,
                    subtitle = context.characterTypeLabel(entry.type),
                    imageUrl = ProxerUrls.entryImage(entry.id),
                    onClick = { navigator.openMedia(entry.id, entry.name) }
                )
            }
        )
    ).filter { it.items.isNotEmpty() }
)
