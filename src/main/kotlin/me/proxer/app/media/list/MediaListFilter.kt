package me.proxer.app.media.list

import me.proxer.app.media.LocalTag
import me.proxer.library.enums.Category
import me.proxer.library.enums.FskConstraint
import me.proxer.library.enums.Language
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType

/**
 * The search criteria of the anime and manga lists.
 */
data class MediaListFilter(
    val type: MediaType,
    val sortCriteria: MediaSearchSortCriteria = MediaSearchSortCriteria.RATING,
    val searchQuery: String? = null,
    val language: Language? = null,
    val genres: List<LocalTag> = emptyList(),
    val excludedGenres: List<LocalTag> = emptyList(),
    val fskConstraints: Set<FskConstraint> = emptySet(),
    val tags: List<LocalTag> = emptyList(),
    val excludedTags: List<LocalTag> = emptyList(),
    val includeUnratedTags: Boolean = false,
    val includeSpoilerTags: Boolean = false,
    val hideFinished: Boolean = false
) {

    companion object {
        /** The types which can be selected for the [category], the first is the default. */
        fun typesFor(category: Category) = when (category) {
            Category.ANIME -> listOf(
                MediaType.ALL_ANIME,
                MediaType.ANIMESERIES,
                MediaType.MOVIE,
                MediaType.OVA,
                MediaType.HENTAI
            )
            Category.MANGA, Category.NOVEL -> listOf(
                MediaType.ALL_MANGA,
                MediaType.MANGASERIES,
                MediaType.ONESHOT,
                MediaType.DOUJIN,
                MediaType.HMANGA
            )
        }

        val sortCriteria = listOf(
            MediaSearchSortCriteria.RATING,
            MediaSearchSortCriteria.CLICKS,
            MediaSearchSortCriteria.EPISODE_AMOUNT,
            MediaSearchSortCriteria.NAME
        )

        fun initial(category: Category, type: String?, sort: String?) = MediaListFilter(
            type = typesFor(category).find { it.name == type } ?: typesFor(category).first(),
            sortCriteria = sortCriteria.find { it.name == sort } ?: MediaSearchSortCriteria.RATING
        )
    }

    /** The amount of extended criteria in use, shown as a badge on the filter button. */
    val extendedCriteriaCount: Int
        get() = listOf(
            language != null,
            genres.isNotEmpty(),
            excludedGenres.isNotEmpty(),
            fskConstraints.isNotEmpty(),
            tags.isNotEmpty(),
            excludedTags.isNotEmpty(),
            includeUnratedTags,
            includeSpoilerTags,
            hideFinished
        ).count { it }

    /** This filter without any extended criteria. */
    fun withoutExtendedCriteria() = MediaListFilter(type = type, sortCriteria = sortCriteria, searchQuery = searchQuery)
}
