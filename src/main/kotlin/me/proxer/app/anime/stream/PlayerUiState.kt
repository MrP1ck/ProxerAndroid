package me.proxer.app.anime.stream

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import kotlinx.coroutines.delay

private const val POSITION_UPDATE_INTERVAL_MS = 250L
private const val SEEK_STEP_MS = 10_000L

/**
 * The state of a [Player] as Compose state, updated by its events. The position is polled while playing.
 */
@Stable
class PlayerUiState(private val player: Player) {

    var isPlaying by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isPlayingAd by mutableStateOf(false)
        private set

    var isSeekable by mutableStateOf(false)
        private set

    var position by mutableLongStateOf(0L)
        private set

    var duration by mutableLongStateOf(0L)
        private set

    internal val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = update()
    }

    internal fun update() {
        isPlaying = player.isPlaying
        isLoading = player.playbackState == Player.STATE_BUFFERING ||
            (player.playbackState == Player.STATE_IDLE && player.playerError == null)
        isPlayingAd = player.isPlayingAd
        isSeekable = player.isCurrentMediaItemSeekable
        position = player.currentPosition.coerceAtLeast(0)
        duration = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0
    }

    fun togglePlayback() {
        player.playWhenReady = !player.playWhenReady

        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0, duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        position = player.currentPosition
    }

    fun rewind() {
        if (isSeekable) seekTo(player.currentPosition - SEEK_STEP_MS)
    }

    fun fastForward() {
        if (isSeekable) seekTo(player.currentPosition + SEEK_STEP_MS)
    }
}

@Composable
fun rememberPlayerUiState(player: Player): PlayerUiState {
    val state = remember(player) { PlayerUiState(player) }

    DisposableEffect(player) {
        state.update()
        player.addListener(state.listener)

        onDispose { player.removeListener(state.listener) }
    }

    LaunchedEffect(player, state.isPlaying) {
        while (state.isPlaying) {
            state.update()

            delay(POSITION_UPDATE_INTERVAL_MS)
        }
    }

    return state
}
