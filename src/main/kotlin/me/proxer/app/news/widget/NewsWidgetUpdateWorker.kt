package me.proxer.app.news.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.proxer.app.R
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.safeInject
import me.proxer.app.util.extension.toInstantBP
import me.proxer.app.widget.WidgetError
import me.proxer.app.widget.WidgetState
import me.proxer.library.ProxerApi
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the news for the [NewsWidget]s and updates them.
 *
 * @author Ruben Gees
 */
class NewsWidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val NAME = "NewsWidgetUpdateWorker"

        private val workManager by safeInject<WorkManager>()

        fun enqueueWork() {
            val workRequest = OneTimeWorkRequestBuilder<NewsWidgetUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            workManager.beginUniqueWork(NAME, ExistingWorkPolicy.KEEP, workRequest).enqueue()
        }
    }

    private val api by safeInject<ProxerApi>()

    override suspend fun doWork(): Result {
        val previous = newsWidgetStore.read(applicationContext)

        update(WidgetState(items = previous?.items.orEmpty(), isLoading = true))

        return try {
            val news = withContext(Dispatchers.IO) {
                api.notifications.news()
                    .build()
                    .safeExecute()
                    .map { SimpleNews(it.id, it.threadId, it.categoryId, it.subject, it.category, it.date.toInstantBP()) }
            }

            update(
                when (news.isEmpty()) {
                    true -> WidgetState(error = WidgetError.from(applicationContext, ErrorAction(R.string.error_no_data_news)))
                    false -> WidgetState(items = news)
                }
            )

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.e(error)

            update(WidgetState(error = WidgetError.from(applicationContext, ErrorUtils.handle(error))))

            Result.failure()
        }
    }

    private suspend fun update(state: WidgetState<SimpleNews>) {
        newsWidgetStore.write(applicationContext, state)

        NewsWidget().updateAll(applicationContext)
        NewsDarkWidget().updateAll(applicationContext)
    }
}
