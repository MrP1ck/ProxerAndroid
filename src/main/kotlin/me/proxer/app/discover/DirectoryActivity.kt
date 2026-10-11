package me.proxer.app.discover

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.extension.startActivity
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Shows an alphabetical, searchable directory of characters, persons, translator groups or industries. Also handles the
 * deep links to the lists of the website.
 */
class DirectoryActivity : ComposeActivity() {

    companion object {
        private const val KIND_EXTRA = "kind"

        fun navigateTo(context: Activity, kind: DirectoryKind) {
            context.startActivity<DirectoryActivity>(KIND_EXTRA to kind)
        }
    }

    private val kind: DirectoryKind
        get() = when (intent.data?.pathSegments?.firstOrNull()) {
            "translatorgroups" -> DirectoryKind.TRANSLATOR_GROUPS
            "industry" -> DirectoryKind.INDUSTRIES
            else -> intent.getSerializableExtra(KIND_EXTRA) as DirectoryKind? ?: DirectoryKind.CHARACTERS
        }

    @Composable
    override fun Content() {
        val navigator = LocalAppNavigator.current
        val linkedId = intent.data?.getQueryParameter("id")

        // The website uses the same pages for the list and the details of a group or industry.
        if (linkedId != null) {
            LaunchedEffect(linkedId) {
                when (kind) {
                    DirectoryKind.TRANSLATOR_GROUPS -> navigator.openTranslatorGroup(linkedId)
                    else -> navigator.openIndustry(linkedId)
                }

                finish()
            }

            return
        }

        val viewModel = koinViewModel<DirectoryViewModel>(key = kind.name) { parametersOf(kind) }

        DirectoryScreen(
            kind = kind,
            state = viewModel.collectContentState(),
            onFilterChange = viewModel::updateFilter,
            onEntryClick = { navigator.openDirectoryEntry(kind, it) },
            onNavigateUp = navigator::navigateUp,
            onLoadMore = viewModel::loadIfPossible,
            onRefresh = viewModel::refresh,
            onErrorAction = rememberErrorActionHandler(viewModel::load)
        )
    }
}
