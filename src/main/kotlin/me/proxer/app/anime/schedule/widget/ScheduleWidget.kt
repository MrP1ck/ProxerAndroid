package me.proxer.app.anime.schedule.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import me.proxer.app.MainActivity
import me.proxer.app.MainSection
import me.proxer.app.R
import me.proxer.app.media.MediaActivity
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.app.widget.ProxerGlanceTheme
import me.proxer.app.widget.ProxerWidgetContent
import me.proxer.app.widget.WidgetItem
import me.proxer.app.widget.WidgetState
import me.proxer.app.widget.WidgetStore
import me.proxer.library.enums.Category
import org.threeten.bp.Instant
import org.threeten.bp.format.DateTimeFormatter

internal val scheduleWidgetStore = WidgetStore("schedule", SimpleCalendarEntry::class.java)

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The anime airing today. Entries which already aired are shown after the upcoming ones. The data is loaded by the
 * [ScheduleWidgetUpdateWorker].
 */
abstract class BaseScheduleWidget(private val isDark: Boolean) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateFlow = scheduleWidgetStore.flow(context)

        if (stateFlow.value == null) ScheduleWidgetUpdateWorker.enqueueWork()

        provideContent {
            val state by stateFlow.collectAsState()

            ProxerGlanceTheme(isDark) { Content(state) }
        }
    }

    @Composable
    private fun Content(state: WidgetState<SimpleCalendarEntry>?) {
        val context = LocalContext.current
        val now = Instant.now()

        // Glance lists can't be scrolled to the next entry, so the upcoming entries are moved to the top instead.
        val orderedState = state?.copy(items = state.items.sortedBy { it.date.isBefore(now) })

        ProxerWidgetContent(
            title = context.getString(R.string.widget_schedule_title),
            state = orderedState,
            onTitleClick = actionStartActivity(MainActivity.getSectionIntent(context, MainSection.SCHEDULE)),
            onRefresh = actionRunCallback<RefreshScheduleAction>(),
            itemKey = { it.id.toLongOrNull() ?: it.id.hashCode().toLong() }
        ) { entry ->
            WidgetItem(
                overline = entry.date.toLocalDateTime().format(timeFormatter),
                title = entry.name,
                subtitle = context.getString(R.string.fragment_schedule_episode, entry.episode.toString()),
                onClick = actionStartActivity(
                    MediaActivity.getIntent(context, entry.entryId, entry.name, Category.ANIME)
                )
            )
        }
    }
}

class ScheduleWidget : BaseScheduleWidget(isDark = false)

class ScheduleDarkWidget : BaseScheduleWidget(isDark = true)

class RefreshScheduleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ScheduleWidgetUpdateWorker.enqueueWork()
    }
}

/**
 * The receiver of the schedule widget. The class name must stay the same, so widgets placed by older versions keep
 * working.
 */
class ScheduleWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        ScheduleWidgetUpdateWorker.enqueueWork()
    }
}

/**
 * The receiver of the dark schedule widget, which uses dark colors regardless of the system setting.
 */
class ScheduleWidgetDarkProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScheduleDarkWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        ScheduleWidgetUpdateWorker.enqueueWork()
    }
}
