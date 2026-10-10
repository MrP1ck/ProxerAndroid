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
data object ChatGraph

@Serializable
data object NewsRoute

/**
 * The anime list. [type] and [sort] are the names of the MediaType and MediaSearchSortCriteria to start with, e.g.
 * from a deep link.
 */
@Serializable
data class AnimeRoute(val type: String? = null, val sort: String? = null)

/**
 * The manga list. See [AnimeRoute] for the parameters.
 */
@Serializable
data class MangaRoute(val type: String? = null, val sort: String? = null)

@Serializable
data object ScheduleRoute

@Serializable
data object BookmarksRoute

@Serializable
data class ChatRoute(val showMessenger: Boolean = false)
