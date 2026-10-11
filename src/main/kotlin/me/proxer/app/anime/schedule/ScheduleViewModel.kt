package me.proxer.app.anime.schedule

import me.proxer.app.base.BaseViewModel
import me.proxer.app.util.extension.await
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay

/**
 * @author Ruben Gees
 */
class ScheduleViewModel : BaseViewModel<Map<CalendarDay, List<CalendarEntry>>>() {

    override suspend fun loadData(): Map<CalendarDay, List<CalendarEntry>> {
        validate()

        return api.media.calendar().await()
            .groupBy { it.weekDay }
            .mapValues { (_, calendarEntries) -> calendarEntries.sortedBy { it.date } }
    }
}
