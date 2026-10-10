package me.proxer.app.anime.stream

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filterNotNull
import me.proxer.app.R
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

private val ScrimColor = Color.Black.copy(alpha = 0.6f)
private val PreviewWidth = 160.dp
private val PreviewHeight = 90.dp

/**
 * What a vertical swipe on the side of the player changes.
 */
enum class SwipeTarget { BRIGHTNESS, VOLUME }

/**
 * The gestures of the player: a tap toggles the controls, a double tap on the left or right half seeks by ten seconds
 * and a vertical swipe on the left or right third changes the brightness or the volume. [onSwipe] receives the change
 * as a fraction of the height (positive is up).
 */
fun Modifier.playerGestures(
    onTap: () -> Unit,
    onDoubleTap: (isForward: Boolean) -> Unit,
    onSwipe: (SwipeTarget, Float) -> Unit,
    onSwipeEnd: () -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { offset -> onDoubleTap(offset.x > size.width / 2) }
        )
    }
    .pointerInput(Unit) {
        var target: SwipeTarget? = null

        detectVerticalDragGestures(
            onDragStart = { offset -> target = swipeTargetAt(offset, size.width.toFloat(), size.height.toFloat()) },
            onDragEnd = {
                target = null
                onSwipeEnd()
            },
            onDragCancel = {
                target = null
                onSwipeEnd()
            },
            onVerticalDrag = { change, dragAmount ->
                target?.let {
                    change.consume()
                    onSwipe(it, -dragAmount / size.height)
                }
            }
        )
    }

private fun swipeTargetAt(offset: Offset, width: Float, height: Float): SwipeTarget? {
    // Ignore swipes near the edges, which are used for the system gestures.
    val isInsideMargins = offset.y > height / 8 && offset.y < height - height / 8 &&
        offset.x > width / 16 && offset.x < width - width / 16

    return when {
        !isInsideMargins -> null
        offset.x < width / 3 -> SwipeTarget.BRIGHTNESS
        offset.x > width / 3 * 2 -> SwipeTarget.VOLUME
        else -> null
    }
}

/**
 * The amount of seconds seeked by double taps, shown on the side of the player.
 */
@Composable
fun SeekIndicator(seconds: Int?, isForward: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(seconds != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Surface(shape = CircleShape, color = ScrimColor, contentColor = Color.White) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(if (isForward) R.drawable.ic_symbol_fast_forward else R.drawable.ic_symbol_fast_rewind),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )

                Text(stringResource(R.string.exoplayer_seek_indicator, seconds ?: 0), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/**
 * The volume or brightness while swiping, from 0 to 1.
 */
@Composable
fun LevelIndicator(target: SwipeTarget?, level: Float, modifier: Modifier = Modifier) {
    AnimatedVisibility(target != null, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        val icon = when (target) {
            SwipeTarget.VOLUME -> when {
                level <= 0f -> R.drawable.ic_symbol_volume_off
                level < 0.5f -> R.drawable.ic_symbol_volume_down
                else -> R.drawable.ic_symbol_volume_up
            }
            else -> when {
                level < 0.33f -> R.drawable.ic_symbol_brightness_low
                level < 0.66f -> R.drawable.ic_symbol_brightness_medium
                else -> R.drawable.ic_symbol_brightness_high
            }
        }

        Surface(shape = RoundedCornerShape(16.dp), color = ScrimColor, contentColor = Color.White) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(40.dp))

                Spacer(Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { level.coerceIn(0f, 1f) },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.width(96.dp)
                )
            }
        }
    }
}

/**
 * The controls shown over the video: the top bar with the [actions], play/pause and seeking in the center and the
 * progress at the bottom. While seeking, [loadPreview] is used to show a frame of the target position.
 */
@Composable
fun StreamControls(
    state: PlayerUiState,
    title: String,
    subtitle: String?,
    isLandscape: Boolean,
    onNavigateUp: () -> Unit,
    onToggleOrientation: () -> Unit,
    onSeekingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    loadPreview: (suspend (positionMs: Long, width: Int, height: Int) -> Bitmap?)? = null,
    actions: @Composable () -> Unit = {}
) {
    var seekPosition by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ScrimColor, Color.Transparent, Color.Transparent, ScrimColor)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateUp, colors = whiteIconButtonColors()) {
                Icon(painterResource(R.drawable.ic_symbol_arrow_back), stringResource(R.string.action_navigate_up))
            }

            Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (subtitle != null) {
                    Text(subtitle, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }

            actions()
        }

        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(R.drawable.ic_symbol_replay_10, R.string.exoplayer_rewind_description, state.isSeekable, state::rewind)

            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(56.dp))
                } else {
                    IconButton(
                        onClick = state::togglePlayback,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isPlaying) R.drawable.ic_symbol_pause_filled else R.drawable.ic_symbol_play_arrow_filled
                            ),
                            contentDescription = stringResource(
                                if (state.isPlaying) R.string.exoplayer_pause_description else R.string.exoplayer_play_description
                            ),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            ControlButton(
                R.drawable.ic_symbol_forward_10,
                R.string.exoplayer_fast_forward_description,
                state.isSeekable,
                state::fastForward
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SeekPreview(seekPosition, state.duration, loadPreview)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(seekPosition ?: state.position), color = Color.White, style = MaterialTheme.typography.labelMedium)

                Slider(
                    value = (seekPosition ?: state.position).toFloat(),
                    onValueChange = {
                        if (seekPosition == null) onSeekingChange(true)

                        seekPosition = it.toLong()
                    },
                    onValueChangeFinished = {
                        seekPosition?.let { state.seekTo(it) }
                        seekPosition = null

                        onSeekingChange(false)
                    },
                    enabled = state.isSeekable && state.duration > 0,
                    valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                Text(formatTime(state.duration), color = Color.White, style = MaterialTheme.typography.labelMedium)

                IconButton(onClick = onToggleOrientation, colors = whiteIconButtonColors()) {
                    Icon(
                        painterResource(if (isLandscape) R.drawable.ic_symbol_fullscreen_exit else R.drawable.ic_symbol_fullscreen),
                        stringResource(R.string.exoplayer_fullscreen_description)
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekPreview(
    seekPosition: Long?,
    duration: Long,
    loadPreview: (suspend (positionMs: Long, width: Int, height: Int) -> Bitmap?)?
) {
    val density = LocalDensity.current
    var frame by remember { mutableStateOf<Bitmap?>(null) }

    val currentSeekPosition by rememberUpdatedState(seekPosition)

    if (loadPreview != null) {
        // Loads the frame of the latest position whenever the previous one is done, skipping the positions in between.
        LaunchedEffect(loadPreview) {
            snapshotFlow { currentSeekPosition }
                .filterNotNull()
                .conflate()
                .collect { position ->
                    with(density) { loadPreview(position, PreviewWidth.roundToPx(), PreviewHeight.roundToPx()) }
                        ?.let { frame = it }
                }
        }
    }

    BoxWithConstraints(Modifier.fillMaxWidth().height(if (seekPosition != null) PreviewHeight + 32.dp else 0.dp)) {
        if (seekPosition != null && duration > 0) {
            val fraction = seekPosition.toFloat() / duration
            val maxOffset = with(density) { (maxWidth - PreviewWidth).toPx() }
            val offset = (fraction * maxOffset).roundToInt()

            Column(
                modifier = Modifier.offset { IntOffset(offset, 0) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loadPreview != null) {
                    Box(
                        Modifier
                            .size(PreviewWidth, PreviewHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        frame?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Text(
                    text = formatTime(seekPosition),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(PreviewWidth)
                )
            }
        }
    }
}

@Composable
private fun ControlButton(@DrawableRes icon: Int, description: Int, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, colors = whiteIconButtonColors(), modifier = Modifier.size(56.dp)) {
        Icon(painterResource(icon), contentDescription = stringResource(description), modifier = Modifier.size(36.dp))
    }
}

/**
 * The overflow menu of the player with a single entry per action.
 */
@Composable
fun StreamOverflowMenu(items: List<Pair<String, () -> Unit>>) {
    if (items.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { isExpanded = true }, colors = whiteIconButtonColors()) {
            Icon(painterResource(R.drawable.ic_symbol_more_vert), contentDescription = null)
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            items.forEach { (title, onClick) ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        isExpanded = false

                        onClick()
                    }
                )
            }
        }
    }
}

@Composable
fun whiteIconButtonColors() = IconButtonDefaults.iconButtonColors(
    contentColor = Color.White,
    disabledContentColor = Color.White.copy(alpha = 0.38f)
)

internal fun formatTime(positionMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(positionMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(positionMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(abs(positionMs)) % 60

    return when {
        hours > 0 -> String.format(Locale.GERMANY, "%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format(Locale.GERMANY, "%d:%02d", minutes, seconds)
    }
}
