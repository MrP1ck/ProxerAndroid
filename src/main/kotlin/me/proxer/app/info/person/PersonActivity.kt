package me.proxer.app.info.person

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.info.ProfileDetails
import me.proxer.app.info.ProfileGroup
import me.proxer.app.info.ProfileLink
import me.proxer.app.info.ProfileScreen
import me.proxer.app.info.birthdayLabel
import me.proxer.app.info.countryLabel
import me.proxer.app.info.genderLabel
import me.proxer.app.info.languageLabel
import me.proxer.app.info.personTypeLabel
import me.proxer.app.info.personTypeOrder
import me.proxer.app.info.subjectLabel
import me.proxer.app.info.takeIfKnown
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.map
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.AppNavigator
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.startActivity
import me.proxer.library.entity.info.Person
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Shows a person with the entries they worked on and the characters they voice. Also handles the deep links to
 * persons.
 */
class PersonActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity<PersonActivity>(
                ID_EXTRA to id,
                NAME_EXTRA to name
            )
        }
    }

    private val id: String
        get() = intent.getStringExtra(ID_EXTRA) ?: intent.data?.getQueryParameter("id") ?: "-1"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalAppNavigator.current
        val viewModel = koinViewModel<PersonViewModel> { parametersOf(id) }
        val state = viewModel.collectContentState()

        ProfileScreen(
            initialName = intent.getStringExtra(NAME_EXTRA),
            shareText = R.string.share_person,
            shareUrl = ProxerUrls.personWeb(id),
            state = state.map { it.toDetails(context, navigator) },
            onErrorAction = rememberErrorActionHandler(viewModel::load)
        )
    }
}

private fun Person.toDetails(context: Context, navigator: AppNavigator) = ProfileDetails(
    name = name,
    imageUrl = ProxerUrls.personImage(id),
    facts = listOfNotNull(
        context.genderLabel(gender)?.let { context.getString(R.string.profile_gender) to it },
        context.birthdayLabel(birthday)?.let { context.getString(R.string.profile_birthday) to it },
        birthplace.takeIfKnown()?.let { context.getString(R.string.profile_birthplace) to it },
        countryLabel(nationality.takeIfKnown())?.let { context.getString(R.string.profile_nationality) to it },
        residence.takeIfKnown()?.let { context.getString(R.string.profile_residence) to it },
        bloodType.takeIfKnown()?.let { context.getString(R.string.profile_blood_type) to it },
        occupations.map { context.personTypeLabel(it.type) }.distinct().takeIf { it.isNotEmpty() }?.let {
            context.getString(R.string.profile_occupations) to it.joinToString(", ")
        }
    ),
    names = names.mapNotNull { name ->
        val alternative = name.alternative.takeIfKnown()

        // Names without an alternative writing are the one already shown as title.
        alternative?.let {
            context.getString(R.string.profile_name_default) to
                context.getString(R.string.profile_name_with_alternative, name.name, it)
        }
    },
    descriptions = descriptions
        .filter { it.text.isNotBlank() }
        .map { context.subjectLabel(it.subject) to it.text },
    groups = listOf(
        ProfileGroup(
            title = context.getString(R.string.profile_works),
            items = entries
                .sortedBy { personTypeOrder(it.type) }
                .mapIndexed { index, entry ->
                    ProfileLink(
                        key = "${entry.id}_$index",
                        title = entry.name,
                        subtitle = context.personTypeLabel(entry.type),
                        imageUrl = ProxerUrls.entryImage(entry.id),
                        onClick = { navigator.openMedia(entry.id, entry.name) }
                    )
                }
        ),
        ProfileGroup(
            title = context.getString(R.string.profile_voices),
            items = characters.mapIndexed { index, character ->
                ProfileLink(
                    key = "${character.id}_$index",
                    title = character.name,
                    subtitle = languageLabel(character.language),
                    imageUrl = ProxerUrls.characterImage(character.id),
                    onClick = { navigator.openCharacter(character.id, character.name) }
                )
            }
        )
    ).filter { it.items.isNotEmpty() }
)
