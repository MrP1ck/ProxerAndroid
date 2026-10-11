package me.proxer.app.media.list

import me.proxer.library.enums.Category
import me.proxer.library.enums.MediaSearchSortCriteria
import me.proxer.library.enums.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaListFilterTest {

    @Test
    fun novelsOfferAllNovelTypes() {
        assertEquals(
            listOf(MediaType.ALL_NOVEL, MediaType.LIGHTNOVEL, MediaType.WEBNOVEL, MediaType.VISUALNOVEL),
            MediaListFilter.typesFor(Category.NOVEL)
        )
    }

    @Test
    fun mangaDoesNotOfferNovelTypes() {
        assertEquals(
            listOf(MediaType.ALL_MANGA, MediaType.MANGASERIES, MediaType.ONESHOT, MediaType.DOUJIN, MediaType.HMANGA),
            MediaListFilter.typesFor(Category.MANGA)
        )
    }

    @Test
    fun initialFilterUsesTheGivenNovelType() {
        val filter = MediaListFilter.initial(Category.NOVEL, "WEBNOVEL", "CLICKS")

        assertEquals(MediaType.WEBNOVEL, filter.type)
        assertEquals(MediaSearchSortCriteria.CLICKS, filter.sortCriteria)
    }

    @Test
    fun initialFilterFallsBackToTheDefaultTypeOfTheCategory() {
        assertEquals(MediaType.ALL_NOVEL, MediaListFilter.initial(Category.NOVEL, "MANGASERIES", null).type)
        assertEquals(MediaType.ALL_MANGA, MediaListFilter.initial(Category.MANGA, "LIGHTNOVEL", null).type)
    }
}
