package me.proxer.app.info

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import me.proxer.app.TestApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class ProfileLabelsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun birthdayWithoutYear() {
        assertEquals("5. Mai", context.birthdayLabel("0000-05-05"))
    }

    @Test
    fun birthdayWithYear() {
        assertEquals("1. Januar 1975", context.birthdayLabel("1975-01-01"))
    }

    @Test
    fun birthdayWithOnlyYear() {
        assertEquals("1975", context.birthdayLabel("1975-00-00"))
    }

    @Test
    fun unknownBirthday() {
        assertNull(context.birthdayLabel("0000-00-00"))
        assertNull(context.birthdayLabel(""))
        assertNull(context.birthdayLabel(null))
    }

    @Test
    fun typesAreTranslatedAndUnknownOnesShownAsTheyAre() {
        assertEquals("Hauptcharakter", context.characterTypeLabel("main"))
        assertEquals("Sprecher", context.personTypeLabel("seiuu"))
        assertEquals("Sprecher", context.personTypeLabel("seiyuu"))
        assertEquals("Neuer-typ", context.personTypeLabel("neuer-typ"))
    }

    @Test
    fun languagesUseTheCodesOfTheApi() {
        assertEquals("Japanisch", languageLabel("jp"))
        assertEquals("Koreanisch", languageLabel("kr"))
        assertEquals("Deutsch", languageLabel("de"))
        assertNull(languageLabel(""))
    }

    @Test
    fun unknownValuesAreHidden() {
        assertNull("".takeIfKnown())
        assertNull("0".takeIfKnown())
        assertNull(null.takeIfKnown())
        assertEquals("172", "172".takeIfKnown())
    }

    @Test
    fun importantTypesComeFirst() {
        val sorted = listOf("other", "support", "main").sortedBy(::characterTypeOrder)

        assertEquals(listOf("main", "support", "other"), sorted)
    }
}
