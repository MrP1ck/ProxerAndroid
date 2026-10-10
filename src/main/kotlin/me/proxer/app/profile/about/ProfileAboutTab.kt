package me.proxer.app.profile.about

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import me.proxer.app.R
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.SectionHeader
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.components.rememberLinkifiedText
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.ui.view.ProxerWebView
import me.proxer.app.util.extension.toAppString
import me.proxer.library.entity.user.UserAbout
import me.proxer.library.enums.Gender
import me.proxer.library.enums.RelationshipStatus
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private const val ZERO_DATE = "0000-00-00"

/**
 * The details a user entered about themself and their about page (HTML).
 */
@Composable
fun ProfileAboutTab(
    userId: String?,
    username: String?,
    contentPadding: PaddingValues,
    viewModel: ProfileAboutViewModel = koinViewModel(parameters = { parametersOf(userId, username) })
) {
    val navigator = LocalAppNavigator.current
    val context = LocalContext.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    ContentStateHost(
        state = state,
        onErrorAction = onErrorAction,
        contentPadding = contentPadding,
        emptyMessage = R.string.error_no_data_profile_about,
        isEmpty = { it.generalRows(context).isEmpty() && it.about.isBlank() }
    ) { about ->
        val rows = about.generalRows(context)

        LazyColumn(contentPadding = contentPadding.plus(PaddingValues(vertical = 8.dp))) {
            if (rows.isNotEmpty()) {
                item(key = "general") {
                    SectionHeader(stringResource(R.string.fragment_about_general))

                    Column(Modifier.padding(horizontal = 16.dp)) {
                        rows.forEach { (title, content) ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    stringResource(title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(128.dp)
                                )

                                SelectionContainer(Modifier.weight(1f)) {
                                    Text(
                                        rememberLinkifiedText(content, navigator::showPage),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (about.about.isNotBlank()) {
                item(key = "about") {
                    SectionHeader(stringResource(R.string.fragment_about_about))

                    AndroidView(
                        factory = { viewContext ->
                            ProxerWebView(viewContext).apply {
                                showPageSubject.subscribe { navigator.showPage(it) }
                            }
                        },
                        update = { it.loadHtml(about.about) },
                        onRelease = { it.destroy() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

private fun UserAbout.generalRows(context: Context): List<Pair<Int, String>> {
    val normalizedGender = if (gender == Gender.UNKNOWN) "" else gender.toAppString(context)

    val normalizedRelationshipStatus = when (relationshipStatus) {
        RelationshipStatus.UNKNOWN -> ""
        else -> relationshipStatus.toAppString(context)
    }

    val normalizedBirthday = when (birthday) {
        ZERO_DATE -> ""
        else -> birthday.split("-").let { if (it.size == 3) "${it[2]}.${it[1]}.${it[0]}" else "" }
    }

    return listOf(
        R.string.fragment_about_occupation to occupation,
        R.string.fragment_about_interests to interests,
        R.string.fragment_about_city to city,
        R.string.fragment_about_country to country,
        R.string.fragment_about_gender to normalizedGender,
        R.string.fragment_about_relationship_status to normalizedRelationshipStatus,
        R.string.fragment_about_birthday to normalizedBirthday,
        R.string.fragment_about_website to website,
        R.string.fragment_about_facebook to facebook,
        R.string.fragment_about_youtube to youtube,
        R.string.fragment_about_chatango to chatango,
        R.string.fragment_about_twitter to twitter,
        R.string.fragment_about_skype to skype,
        R.string.fragment_about_deviantart to deviantart
    ).filter { (_, content) -> content.isNotBlank() }
}
