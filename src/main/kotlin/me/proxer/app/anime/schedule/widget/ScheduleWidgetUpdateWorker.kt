package me.proxer.app.anime.schedule.widget

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
import me.proxer.app.util.extension.toLocalDateTimeBP
import me.proxer.app.widget.WidgetError
import me.proxer.app.widget.WidgetState
import me.proxer.library.ProxerApi
import org.threeten.bp.LocalDate
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads the anime airing today for the [ScheduleWidget]s and updates them.
 *
 * @author Ruben Gees
 */
class ScheduleWidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val NAME = "ScheduleWidgetUpdateWorker"

        private val workManager by safeInject<WorkManager>()

        fun enqueueWork() {
            val workRequest = OneTimeWorkRequestBuilder<ScheduleWidgetUpdateWorker>()
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
        val previous = scheduleWidgetStore.read(applicationContext)

        update(WidgetState(items = previous?.items.orEmpty(), isLoading = true))

        return try {
            val entries = withContext(Dispatchers.IO) {
                api.media.calendar()
                    .build()
                    .safeExecute()
                    .filter { it.date.toLocalDateTimeBP().dayOfMonth == LocalDate.now().dayOfMonth }
                    .map {
                        SimpleCalendarEntry(
                            it.id,
                            it.entryId,
                            it.name,
                            it.episode,
                            it.date.toInstantBP(),
                            it.uploadDate.toInstantBP()
                        )
                    }
            }

            update(
                when (entries.isEmpty()) {
                    true -> WidgetState(
                        error = WidgetError.from(applicationContext, ErrorAction(R.string.error_no_data_schedule))
                    )
                    false -> WidgetState(items = entries)
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

    private suspend fun update(state: WidgetState<SimpleCalendarEntry>) {
        scheduleWidgetStore.write(applicationContext, state)

        ScheduleWidget().updateAll(applicationContext)
        ScheduleDarkWidget().updateAll(applicationContext)
    }
}
