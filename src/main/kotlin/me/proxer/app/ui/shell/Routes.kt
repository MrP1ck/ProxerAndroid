package me.proxer.app.ui.shell

import kotlinx.serialization.Serializable

// Each top level destination is a nested graph, so its sub screens keep the destination selected in the navigation bar
// and every destination keeps its own back stack.

@Serializable
data object NewsGraph

@Serializable
data object AnimeGraph

@Serializable
data object MangaGraph

@Serializable
data object BookmarksGraph

@Serializable
data object MessengerGraph

@Serializable
data object NewsRoute

/**
 * The anime list. [type] and [sort] are the names of the MediaType and MediaSearchSortCriteria to start with, e.g.
 * from a deep link.
 */
@Serializable
data class AnimeRoute(val type: String? = null, val sort: String? = null)

/**
 * The manga list, which can be switched to novels. See [AnimeRoute] for the parameters. [category] is the name of the
 * Category to start with.
 */
@Serializable
data class MangaRoute(val type: String? = null, val sort: String? = null, val category: String = "MANGA")

@Serializable
data object ScheduleRoute

@Serializable
data object BookmarksRoute

@Serializable
data object MessengerRoute
