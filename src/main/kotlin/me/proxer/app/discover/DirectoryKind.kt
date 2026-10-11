package me.proxer.app.discover

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import me.proxer.app.R
import me.proxer.library.enums.IndustryType
import okhttp3.HttpUrl

/**
 * The directories of the website, which can be browsed alphabetically and searched.
 */
enum class DirectoryKind(
    @param:StringRes val title: Int,
    @param:StringRes val searchHint: Int,
    @param:DrawableRes val icon: Int
) {
    CHARACTERS(R.string.section_characters, R.string.directory_search_hint_characters, R.drawable.ic_symbol_person),
    PERSONS(R.string.section_persons, R.string.directory_search_hint_persons, R.drawable.ic_symbol_account_circle),
    TRANSLATOR_GROUPS(
        R.string.section_translator_groups,
        R.string.directory_search_hint_translator_groups,
        R.drawable.ic_symbol_group
    ),
    INDUSTRIES(R.string.section_industries, R.string.directory_search_hint_industries, R.drawable.ic_symbol_live_tv)
}

/**
 * An item of a [DirectoryKind].
 */
data class DirectoryEntry(
    val id: String,
    val name: String,
    val imageUrl: HttpUrl?,
    val industryType: IndustryType? = null
)
