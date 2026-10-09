package me.proxer.app.ui.shell

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.app.ShareCompat
import androidx.navigation.NavController
import me.proxer.app.MainActivity
import me.proxer.app.MainSection
import me.proxer.app.R
import me.proxer.app.anime.AnimeActivity
import me.proxer.app.auth.LoginDialog
import me.proxer.app.auth.LogoutDialog
import me.proxer.app.base.BaseActivity
import me.proxer.app.chat.prv.LocalConference
import me.proxer.app.chat.prv.Participant
import me.proxer.app.chat.prv.PrvMessengerActivity
import me.proxer.app.chat.prv.create.CreateConferenceActivity
import me.proxer.app.chat.pub.message.ChatActivity
import me.proxer.app.forum.TopicActivity
import me.proxer.app.info.industry.IndustryActivity
import me.proxer.app.info.translatorgroup.TranslatorGroupActivity
import me.proxer.app.manga.MangaActivity
import me.proxer.app.media.MediaActivity
import me.proxer.app.notification.NotificationActivity
import me.proxer.app.profile.ProfileActivity
import me.proxer.app.profile.settings.ProfileSettingsActivity
import me.proxer.app.settings.AboutActivity
import me.proxer.app.settings.AgeConfirmationDialog
import me.proxer.app.settings.SettingsActivity
import me.proxer.app.ui.ImageDetailActivity
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.enums.Language
import okhttp3.HttpUrl

/**
 * Navigates between the screens of the app. Screens use this instead of starting Activities or using the
 * [NavController] directly, so they don't need to know which screens are already part of the Compose navigation.
 *
 * [navController] is null for screens hosted in their own Activity (see ComposeActivity).
 */
class AppNavigator(private val activity: BaseActivity, private val navController: NavController?) {

    fun navigateUp() {
        if (navController?.navigateUp() != true) activity.finish()
    }

    fun openSchedule() = when (navController) {
        null -> MainActivity.navigateToSection(activity, MainSection.SCHEDULE)
        else -> navController.navigate(ScheduleRoute)
    }

    fun openIndustry(id: String, name: String? = null) = IndustryActivity.navigateTo(activity, id, name)

    fun openTranslatorGroup(id: String, name: String? = null) = TranslatorGroupActivity.navigateTo(activity, id, name)

    fun showAgeConfirmation() = AgeConfirmationDialog.show(activity)

    fun share(text: String) = ShareCompat.IntentBuilder(activity)
        .setText(text)
        .setType("text/plain")
        .setChooserTitle(activity.getString(R.string.share_title))
        .startChooser()

    fun openMedia(id: String, name: String? = null, category: Category? = null) {
        MediaActivity.navigateTo(activity, id, name, category)
    }

    fun openAnimeEpisode(
        id: String,
        episode: Int,
        language: AnimeLanguage,
        name: String? = null,
        episodeAmount: Int? = null
    ) {
        AnimeActivity.navigateTo(activity, id, episode, language, name, episodeAmount)
    }

    fun openMangaChapter(
        id: String,
        episode: Int,
        language: Language,
        chapterTitle: String?,
        name: String? = null,
        episodeAmount: Int? = null
    ) {
        MangaActivity.navigateTo(activity, id, episode, language, chapterTitle, name, episodeAmount)
    }

    fun openTopic(id: String, categoryId: String, topic: String? = null) {
        TopicActivity.navigateTo(activity, id, categoryId, topic)
    }

    fun openImage(url: HttpUrl) = ImageDetailActivity.navigateTo(activity, url)

    fun openConference(conference: LocalConference, initialMessage: String? = null) {
        PrvMessengerActivity.navigateTo(activity, conference, initialMessage)
    }

    fun openCreateConference(isGroup: Boolean, initialParticipant: Participant? = null) {
        CreateConferenceActivity.navigateTo(activity, isGroup, initialParticipant)
    }

    fun openChatRoom(id: String, name: String, isReadOnly: Boolean) {
        ChatActivity.navigateTo(activity, id, name, isReadOnly)
    }

    fun openProfile(userId: String?, username: String?, image: String? = null) {
        ProfileActivity.navigateTo(activity, userId, username, image)
    }

    fun openNotifications() = NotificationActivity.navigateTo(activity)

    fun openProfileSettings() = ProfileSettingsActivity.navigateTo(activity)

    fun openSettings() = SettingsActivity.navigateTo(activity)

    fun openAbout() = AboutActivity.navigateTo(activity)

    fun showLogin() = LoginDialog.show(activity)

    fun showLogout() = LogoutDialog.show(activity)

    fun showPage(url: HttpUrl) = activity.showPage(url, forceBrowser = false, skipCheck = false)
}

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> { error("No AppNavigator provided") }
