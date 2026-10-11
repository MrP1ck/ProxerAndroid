package me.proxer.app.info

import android.content.Context
import androidx.annotation.StringRes
import me.proxer.app.R
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Maps the values of characters and persons, which the API returns as plain strings, to texts for the user. Unknown
 * values are shown as they are, so new ones from the API still make sense.
 */

@StringRes
private fun characterTypeRes(type: String) = when (type) {
    "main" -> R.string.character_type_main
    "support" -> R.string.character_type_support
    else -> null
}

@StringRes
private fun characterGroupRes(type: String) = when (type) {
    "main" -> R.string.character_group_main
    "support" -> R.string.character_group_support
    else -> null
}

@StringRes
private fun personTypeRes(type: String) = when (type) {
    "seiyuu", "seiuu" -> R.string.person_type_seiyuu
    "original-creator" -> R.string.person_type_original_creator
    "director" -> R.string.person_type_director
    "author" -> R.string.person_type_author
    "author-art" -> R.string.person_type_author_art
    "art" -> R.string.person_type_art
    "mangaka" -> R.string.person_type_mangaka
    "misc" -> R.string.person_type_misc
    else -> null
}

@StringRes
private fun subjectRes(subject: String) = when (subject) {
    "intro" -> R.string.profile_subject_intro
    "appearance" -> R.string.profile_subject_appearance
    "personality" -> R.string.profile_subject_personality
    "trivia" -> R.string.profile_subject_trivia
    "skills" -> R.string.profile_subject_skills
    "past" -> R.string.profile_subject_past
    "present" -> R.string.profile_subject_present
    "biography" -> R.string.profile_subject_biography
    "awards" -> R.string.profile_subject_awards
    else -> null
}

private fun Context.labelOrRaw(@StringRes res: Int?, raw: String) = res?.let { getString(it) } ?: raw.replaceFirstChar {
    it.uppercase()
}

fun Context.characterTypeLabel(type: String) = labelOrRaw(characterTypeRes(type), type)

fun Context.characterGroupLabel(type: String) = labelOrRaw(characterGroupRes(type), type)

fun Context.personTypeLabel(type: String) = labelOrRaw(personTypeRes(type), type)

fun Context.subjectLabel(subject: String) = labelOrRaw(subjectRes(subject), subject)

/**
 * The sort position of the character type: the more important characters come first.
 */
fun characterTypeOrder(type: String) = when (type) {
    "main" -> 0
    "support" -> 1
    else -> 2
}

/**
 * The sort position of the person type, the voice actors come last since there are usually many of them.
 */
fun personTypeOrder(type: String) = when (type) {
    "original-creator" -> 0
    "author", "author-art", "art" -> 1
    "director" -> 2
    "misc" -> 4
    "seiyuu", "seiuu" -> 5
    else -> 3
}

fun Context.genderLabel(gender: String?) = when (gender) {
    "m" -> getString(R.string.profile_gender_male)
    "f" -> getString(R.string.profile_gender_female)
    else -> null
}

/**
 * The name of a language code used by the API. It is not always the ISO one, e.g. "jp" and "kr".
 */
fun languageLabel(code: String?): String? {
    val isoCode = when (code) {
        null, "" -> return null
        "jp" -> "ja"
        "kr" -> "ko"
        else -> code
    }

    return Locale(isoCode).getDisplayLanguage(Locale.GERMAN).takeIf { it.isNotBlank() && it != isoCode }
}

/**
 * The name of a country code, for example "JP".
 */
fun countryLabel(code: String?): String? {
    if (code.isNullOrBlank()) return null

    return Locale("", code).getDisplayCountry(Locale.GERMAN).takeIf { it.isNotBlank() && it != code.uppercase() }
        ?: code
}

/**
 * Formats a date like "1975-01-01", where unknown parts are zero, e.g. "0000-05-05" for a character without a year.
 */
fun Context.birthdayLabel(birthday: String?): String? {
    val parts = birthday?.split("-")?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 3 } ?: return null
    val (year, month, day) = parts
    val monthName = if (month in 1..12) Month.of(month).getDisplayName(TextStyle.FULL, Locale.GERMAN) else null

    return when {
        monthName == null -> year.takeIf { it > 0 }?.toString()
        day <= 0 && year > 0 -> "$monthName $year"
        day <= 0 -> monthName
        year > 0 -> getString(R.string.profile_birthday_full, day, monthName, year)
        else -> getString(R.string.profile_birthday_day_month, day, monthName)
    }
}

/**
 * Returns the value if it is useful to show. The API uses empty strings and zeros for unknown values.
 */
fun String?.takeIfKnown() = this?.trim()?.takeIf { it.isNotEmpty() && it != "0" }
