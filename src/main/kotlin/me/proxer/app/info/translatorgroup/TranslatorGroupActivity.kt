package me.proxer.app.info.translatorgroup

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import me.proxer.app.R
import me.proxer.app.base.ComposeActivity
import me.proxer.app.info.OrganisationInfo
import me.proxer.app.info.OrganisationProject
import me.proxer.app.info.OrganisationScreen
import me.proxer.app.info.displayName
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.map
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.startActivity
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toCategory
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Ruben Gees
 */
class TranslatorGroupActivity : ComposeActivity() {

    companion object {
        private const val ID_EXTRA = "id"
        private const val NAME_EXTRA = "name"

        fun navigateTo(context: Activity, id: String, name: String? = null) {
            context.startActivity<TranslatorGroupActivity>(
                ID_EXTRA to id,
                NAME_EXTRA to name
            )
        }
    }

    private val id: String
        get() = intent.getSafeStringExtra(ID_EXTRA)

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val infoViewModel = koinViewModel<TranslatorGroupInfoViewModel> { parametersOf(id) }
        val projectViewModel = koinViewModel<TranslatorGroupProjectViewModel> { parametersOf(id) }
        val infoState = infoViewModel.collectContentState()
        val projectsState = projectViewModel.collectContentState()

        OrganisationScreen(
            initialName = intent.getStringExtra(NAME_EXTRA),
            shareText = R.string.share_translator_group,
            shareUrl = ProxerUrls.translatorGroupWeb(id),
            infoState = infoState.map { info ->
                OrganisationInfo(
                    name = info.name,
                    imageUrl = if (info.image.isBlank()) null else ProxerUrls.translatorGroupImage(id),
                    rows = listOfNotNull(
                        info.country.displayName()?.let { context.getString(R.string.fragment_translator_group_language) to it }
                    ),
                    link = info.link,
                    description = info.description
                )
            },
            onInfoErrorAction = rememberErrorActionHandler(infoViewModel::load),
            projectsState = projectsState.map { projects ->
                projects.map { project ->
                    OrganisationProject(
                        id = project.id,
                        name = project.name,
                        subtitle = project.medium.toAppString(context) + " · " + project.state.toAppString(context),
                        category = project.medium.toCategory(),
                        rating = project.rating,
                        coverUrl = ProxerUrls.entryImage(project.id)
                    )
                }
            },
            onProjectsErrorAction = rememberErrorActionHandler(projectViewModel::load),
            onLoadMoreProjects = projectViewModel::loadIfPossible,
            onRefreshProjects = projectViewModel::refresh
        )
    }
}
