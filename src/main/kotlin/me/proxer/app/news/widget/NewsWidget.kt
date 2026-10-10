package me.proxer.app.news.widget

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
import me.proxer.app.forum.TopicActivity
import me.proxer.app.util.extension.distanceInWordsToNow
import me.proxer.app.util.extension.toLocalDateTime
import me.proxer.app.widget.ProxerGlanceTheme
import me.proxer.app.widget.ProxerWidgetContent
import me.proxer.app.widget.WidgetItem
import me.proxer.app.widget.WidgetState
import me.proxer.app.widget.WidgetStore

internal val newsWidgetStore = WidgetStore("news", SimpleNews::class.java)

/**
 * The latest news. The data is loaded by the [NewsWidgetUpdateWorker].
 */
abstract class BaseNewsWidget(private val isDark: Boolean) : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateFlow = newsWidgetStore.flow(context)

        if (stateFlow.value == null) NewsWidgetUpdateWorker.enqueueWork()

        provideContent {
            val state by stateFlow.collectAsState()

            ProxerGlanceTheme(isDark) { Content(state) }
        }
    }

    @Composable
    private fun Content(state: WidgetState<SimpleNews>?) {
        val context = LocalContext.current

        ProxerWidgetContent(
            title = context.getString(R.string.widget_news_title),
            state = state,
            onTitleClick = actionStartActivity(MainActivity.getSectionIntent(context, MainSection.NEWS)),
            onRefresh = actionRunCallback<RefreshNewsAction>(),
            itemKey = { it.id.toLongOrNull() ?: it.id.hashCode().toLong() }
        ) { news ->
            WidgetItem(
                title = news.subject,
                subtitle = context.getString(
                    R.string.widget_news_info,
                    news.date.toLocalDateTime().distanceInWordsToNow(context),
                    news.category
                ),
                onClick = actionStartActivity(
                    TopicActivity.getIntent(context, news.threadId, news.categoryId, news.subject)
                )
            )
        }
    }
}

class NewsWidget : BaseNewsWidget(isDark = false)

class NewsDarkWidget : BaseNewsWidget(isDark = true)

class RefreshNewsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        NewsWidgetUpdateWorker.enqueueWork()
    }
}

/**
 * The receiver of the news widget. The class name must stay the same, so widgets placed by older versions keep working.
 */
class NewsWidgetProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        NewsWidgetUpdateWorker.enqueueWork()
    }
}

/**
 * The receiver of the dark news widget, which uses dark colors regardless of the system setting.
 */
class NewsWidgetDarkProvider : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NewsDarkWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        NewsWidgetUpdateWorker.enqueueWork()
    }
}
