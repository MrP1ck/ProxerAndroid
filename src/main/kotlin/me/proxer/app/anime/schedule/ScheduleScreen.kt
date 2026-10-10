package me.proxer.app.anime.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import me.proxer.app.R
import me.proxer.app.ui.components.COVER_ASPECT_RATIO
import me.proxer.app.ui.components.ContentState
import me.proxer.app.ui.components.ContentStateHost
import me.proxer.app.ui.components.ProxerAsyncImage
import me.proxer.app.ui.components.ProxerScaffold
import me.proxer.app.ui.components.RatingBadge
import me.proxer.app.ui.components.collectContentState
import me.proxer.app.ui.components.plus
import me.proxer.app.ui.components.rememberErrorActionHandler
import me.proxer.app.ui.preview.PreviewData
import me.proxer.app.ui.preview.PreviewSurface
import me.proxer.app.ui.preview.ThemePreviews
import me.proxer.app.ui.shell.LocalAppNavigator
import me.proxer.app.util.ErrorUtils.ErrorAction
import me.proxer.app.util.extension.formattedDistanceTo
import me.proxer.app.util.extension.toAppString
import me.proxer.app.util.extension.toLocalDateTimeBP
import me.proxer.library.entity.media.CalendarEntry
import me.proxer.library.enums.CalendarDay
import me.proxer.library.util.ProxerUrls
import org.koin.androidx.compose.koinViewModel
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * The airing times of the anime this week, grouped by day. Starts at today.
 */
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = koinViewModel()) {
    val navigator = LocalAppNavigator.current
    val state = viewModel.collectContentState()
    val onErrorAction = rememberErrorActionHandler(viewModel::load)

    ProxerScaffold(title = stringResource(R.string.section_schedule), onNavigateUp = navigator::navigateUp) { padding ->
        ScheduleContent(
            state = state,
            onRefresh = viewModel::refresh,
            onErrorAction = onErrorAction,
            onEntryClick = { navigator.openMedia(it.entryId, it.name) },
            contentPadding = padding
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleContent(
    state: ContentState<Map<CalendarDay, List<CalendarEntry>>>,
    onRefresh: () -> Unit,
    onErrorAction: (ErrorAction) -> Unit,
    onEntryClick: (CalendarEntry) -> Unit,
    contentPadding: PaddingValues
) {
    var isRefreshRequested by rememberSaveable { mutableStateOf(false) }

    if (!state.isLoading) isRefreshRequested = false

    PullToRefreshBox(
        isRefreshing = isRefreshRequested && state.isLoading,
        onRefresh = {
            isRefreshRequested = true
            onRefresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        ContentStateHost(
            state = state,
            onErrorAction = onErrorAction,
            contentPadding = contentPadding,
            emptyMessage = R.string.error_no_data_schedule,
            isEmpty = { it.isEmpty() }
        ) { schedule ->
            val days = remember(schedule) { schedule.toList().sortedBy { (day) -> day.daysFromToday() } }
            val listState = rememberLazyListState()
            val now by rememberCurrentTime()

            LazyColumn(
                state = listState,
                contentPadding = contentPadding.plus(PaddingValues(vertical = 8.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(days, key = { (day) -> day.name }) { (day, entries) ->
                    ScheduleDay(day, entries, now, onEntryClick)
                }
            }
        }
    }
}

@Composable
private fun ScheduleDay(
    day: CalendarDay,
    entries: List<CalendarEntry>,
    now: LocalDateTime,
    onEntryClick: (CalendarEntry) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = day.toAppString(context),
            style = MaterialTheme.typography.titleMedium,
            color = when (day.daysFromToday() == 0) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (day.daysFromToday() == 0) FontWeight.Bold else null,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                ScheduleCard(entry, now, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun ScheduleCard(entry: CalendarEntry, now: LocalDateTime, onClick: () -> Unit) {
    val airingDateTime = entry.date.toLocalDateTimeBP()
    val uploadDateTime = entry.uploadDate.toLocalDateTimeBP()

    Card(onClick = onClick, modifier = Modifier.width(160.dp)) {
        Box {
            ProxerAsyncImage(
                url = ProxerUrls.entryImage(entry.entryId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COVER_ASPECT_RATIO)
            )

            if (entry.rating > 0) {
                RatingBadge(entry.rating, Modifier.padding(8.dp))
            }
        }

        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = stringResource(R.string.fragment_schedule_episode, entry.episode.toString()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = hourMinuteFormatter.format(airingDateTime),
                style = MaterialTheme.typography.labelLarge
            )

            val isUploaded = uploadDateTime.isBefore(now)

            Text(
                text = when {
                    isUploaded && entry.date == entry.uploadDate -> stringResource(R.string.fragment_schedule_aired)
                    isUploaded -> stringResource(R.string.fragment_schedule_uploaded)
                    airingDateTime.isBefore(now) -> stringResource(
                        R.string.fragment_schedule_aired_remaining_time,
                        now.formattedDistanceTo(uploadDateTime)
                    )
                    else -> stringResource(
                        R.string.fragment_schedule_remaining_time,
                        now.formattedDistanceTo(airingDateTime)
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (isUploaded) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                minLines = 2
            )
        }
    }
}

/**
 * The current time, updated every second for the countdowns.
 */
@Composable
private fun rememberCurrentTime() = remember { mutableStateOf(LocalDateTime.now()) }.also { state ->
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)

            state.value = LocalDateTime.now()
        }
    }
}

private fun CalendarDay.daysFromToday(): Int {
    val dayOfWeek = when (this) {
        CalendarDay.MONDAY -> DayOfWeek.MONDAY
        CalendarDay.TUESDAY -> DayOfWeek.TUESDAY
        CalendarDay.WEDNESDAY -> DayOfWeek.WEDNESDAY
        CalendarDay.THURSDAY -> DayOfWeek.THURSDAY
        CalendarDay.FRIDAY -> DayOfWeek.FRIDAY
        CalendarDay.SATURDAY -> DayOfWeek.SATURDAY
        CalendarDay.SUNDAY -> DayOfWeek.SUNDAY
    }

    return Math.floorMod(dayOfWeek.value - LocalDate.now().dayOfWeek.value, DayOfWeek.values().size)
}

@ThemePreviews
@Composable
private fun ScheduleContentPreview() = PreviewSurface {
    ScheduleContent(
        state = ContentState(PreviewData.calendarEntries.groupBy { it.weekDay }),
        onRefresh = {},
        onErrorAction = {},
        onEntryClick = {},
        contentPadding = PaddingValues()
    )
}
