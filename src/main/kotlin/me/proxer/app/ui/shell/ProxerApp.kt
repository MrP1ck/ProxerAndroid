package me.proxer.app.ui.shell

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.Flow
import me.proxer.app.MainSection
import me.proxer.app.R
import me.proxer.app.anime.schedule.ScheduleScreen
import me.proxer.app.base.BaseActivity
import me.proxer.app.bookmark.BookmarkScreen
import me.proxer.app.chat.ChatScreen
import me.proxer.app.media.list.MediaListScreen
import me.proxer.app.news.NewsScreen
import me.proxer.app.ui.components.LocalSnackbarHostState
import me.proxer.library.enums.Category
import kotlin.reflect.KClass

/**
 * A request to show a section, e.g. from a deep link or notification. [type] and [sort] are only used for the anime and
 * manga lists.
 */
data class SectionRequest(val section: MainSection, val type: String? = null, val sort: String? = null)

private enum class TopLevelDestination(
    val graph: Any,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int
) {
    NEWS(NewsGraph, R.string.section_news, R.drawable.ic_symbol_newspaper, R.drawable.ic_symbol_newspaper_filled),
    ANIME(AnimeGraph, R.string.section_anime, R.drawable.ic_symbol_live_tv, R.drawable.ic_symbol_live_tv_filled),
    MANGA(MangaGraph, R.string.section_manga, R.drawable.ic_symbol_menu_book, R.drawable.ic_symbol_menu_book_filled),
    BOOKMARKS(
        BookmarksGraph,
        R.string.section_bookmarks,
        R.drawable.ic_symbol_bookmark,
        R.drawable.ic_symbol_bookmark_filled
    ),
    CHAT(ChatGraph, R.string.section_chat, R.drawable.ic_symbol_chat, R.drawable.ic_symbol_chat_filled);

    val graphClass: KClass<*> get() = graph::class
}

/**
 * The app: the top level destinations in a navigation bar (or rail on larger screens) and the navigation between the
 * screens.
 */
@Composable
fun ProxerApp(
    activity: BaseActivity,
    startSection: MainSection,
    sectionRequests: Flow<SectionRequest>,
    snackbarHostState: SnackbarHostState
) {
    val navController = rememberNavController()
    val navigator = remember(activity, navController) { AppNavigator(activity, navController) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    var isAccountSheetVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(navController) {
        sectionRequests.collect { request -> navController.showSection(request, navigator) }
    }

    CompositionLocalProvider(
        LocalAppNavigator provides navigator,
        LocalSnackbarHostState provides snackbarHostState,
        LocalOpenAccountSheet provides { isAccountSheetVisible = true }
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                TopLevelDestination.values().forEach { destination ->
                    val isSelected = backStackEntry?.destination?.hierarchy
                        ?.any { it.hasRoute(destination.graphClass) } == true

                    item(
                        selected = isSelected,
                        onClick = { navController.navigateToTopLevel(destination.graph) },
                        icon = {
                            Icon(
                                painter = painterResource(if (isSelected) destination.selectedIcon else destination.icon),
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(destination.label),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        }
                    )
                }
            }
        ) {
            NavHost(navController = navController, startDestination = startSection.toTopLevelDestination().graph) {
                navigation<NewsGraph>(startDestination = NewsRoute) {
                    composable<NewsRoute> { NewsScreen() }
                }

                navigation<AnimeGraph>(startDestination = AnimeRoute()) {
                    composable<AnimeRoute> {
                        val route = it.toRoute<AnimeRoute>()

                        MediaListScreen(Category.ANIME, route.type, route.sort)
                    }

                    composable<ScheduleRoute> { ScheduleScreen() }
                }

                navigation<MangaGraph>(startDestination = MangaRoute()) {
                    composable<MangaRoute> {
                        val route = it.toRoute<MangaRoute>()

                        MediaListScreen(Category.MANGA, route.type, route.sort)
                    }
                }

                navigation<BookmarksGraph>(startDestination = BookmarksRoute) {
                    composable<BookmarksRoute> { BookmarkScreen() }
                }

                navigation<ChatGraph>(startDestination = ChatRoute()) {
                    composable<ChatRoute> { ChatScreen(showMessenger = it.toRoute<ChatRoute>().showMessenger) }
                }
            }
        }

        if (isAccountSheetVisible) {
            AccountSheet(onDismiss = { isAccountSheetVisible = false })
        }
    }
}

private fun MainSection.toTopLevelDestination() = when (this) {
    MainSection.NEWS, MainSection.INFO, MainSection.SETTINGS -> TopLevelDestination.NEWS
    MainSection.CHAT, MainSection.MESSENGER -> TopLevelDestination.CHAT
    MainSection.BOOKMARKS -> TopLevelDestination.BOOKMARKS
    MainSection.ANIME, MainSection.SCHEDULE -> TopLevelDestination.ANIME
    MainSection.MANGA -> TopLevelDestination.MANGA
}

/**
 * Switches to a top level destination, keeping the state of the others. Going back from any destination leads to the
 * start destination first, like with the previous drawer navigation.
 */
private fun NavController.navigateToTopLevel(graph: Any) = navigate(graph) {
    popUpTo(this@navigateToTopLevel.graph.findStartDestination().id) { saveState = true }

    launchSingleTop = true
    restoreState = true
}

private fun NavController.showSection(request: SectionRequest, navigator: AppNavigator) {
    when (request.section) {
        MainSection.INFO -> navigator.openAbout()
        MainSection.SETTINGS -> navigator.openSettings()
        MainSection.SCHEDULE -> {
            navigateToTopLevel(AnimeGraph)
            navigate(ScheduleRoute) { launchSingleTop = true }
        }
        MainSection.ANIME -> when (request.type != null || request.sort != null) {
            true -> {
                navigateToTopLevel(AnimeGraph)
                navigate(AnimeRoute(request.type, request.sort)) { popUpTo<AnimeRoute> { inclusive = true } }
            }
            false -> navigateToTopLevel(AnimeGraph)
        }
        MainSection.MANGA -> when (request.type != null || request.sort != null) {
            true -> {
                navigateToTopLevel(MangaGraph)
                navigate(MangaRoute(request.type, request.sort)) { popUpTo<MangaRoute> { inclusive = true } }
            }
            false -> navigateToTopLevel(MangaGraph)
        }
        MainSection.MESSENGER -> navigate(ChatRoute(showMessenger = true)) {
            popUpTo(graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
        else -> navigateToTopLevel(request.section.toTopLevelDestination().graph)
    }
}
