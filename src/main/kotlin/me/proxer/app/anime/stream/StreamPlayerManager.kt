package me.proxer.app.anime.stream

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AdViewProvider
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ima.ImaAdsLoader
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.google.android.gms.cast.framework.CastContext
import me.proxer.app.MainApplication.Companion.USER_AGENT
import me.proxer.app.util.ErrorUtils
import okhttp3.OkHttpClient

/**
 * The players of the stream: the local [ExoPlayer] and, if Google Cast is available, a [CastPlayer]. While a cast
 * session is active, the [currentPlayer] is the cast player.
 *
 * [currentPlayer] and [error] are Compose state.
 */
@OptIn(UnstableApi::class)
class StreamPlayerManager(
    context: Context,
    client: OkHttpClient,
    castContext: CastContext?,
    private var media: StreamMedia
) {

    /**
     * The view showing the UI of ads. Set by the player view once it is created. With ads, the local player is only
     * prepared once this is set, since the ads need it.
     */
    var adViewGroup: ViewGroup? = null
        set(value) {
            field = value

            if (value != null && !isPrepared) prepareLocalPlayer()
        }

    private val adsLoader: ImaAdsLoader? = media.adTag?.let { ImaAdsLoader.Builder(context).build() }

    private val dataSourceFactory = OkHttpDataSource.Factory(client).setUserAgent(USER_AGENT)

    private val localPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(dataSourceFactory).apply {
                if (adsLoader != null) {
                    setLocalAdInsertionComponents(
                        { adsLoader },
                        object : AdViewProvider {
                            override fun getAdViewGroup(): ViewGroup =
                                requireNotNull(this@StreamPlayerManager.adViewGroup) { "No view for ads set" }
                        }
                    )
                }
            }
        )
        .setAudioAttributes(
            AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).setUsage(C.USAGE_MEDIA).build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()

    private val castPlayer: CastPlayer? = castContext?.let { CastPlayer(it) }

    var currentPlayer by mutableStateOf<Player>(localPlayer)
        private set

    var error by mutableStateOf<ErrorUtils.ErrorAction?>(null)
        private set

    val isPlayingAd get() = localPlayer.isPlayingAd

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            lastPosition = currentPlayer.currentPosition
            this@StreamPlayerManager.error = ErrorUtils.handle(error)
        }
    }

    private var isPrepared = false
    private var isStarted = false
    private var isFirstStart = true
    private var wasPlaying = false
    private var lastPosition = C.TIME_UNSET

    init {
        updateReferer()

        adsLoader?.setPlayer(localPlayer)
        localPlayer.addListener(listener)

        if (adsLoader == null) prepareLocalPlayer()

        castPlayer?.addListener(listener)
        castPlayer?.setSessionAvailabilityListener(
            object : SessionAvailabilityListener {
                override fun onCastSessionAvailable() = switchTo(castPlayer)
                override fun onCastSessionUnavailable() = switchTo(localPlayer)
            }
        )

        if (castPlayer?.isCastSessionAvailable == true) {
            switchTo(castPlayer)
        }
    }

    /**
     * Starts playing on the first start (at [initialPosition] if set) or if the player was playing when it was
     * stopped.
     */
    fun start(initialPosition: Long?) {
        isStarted = true

        if (isFirstStart && initialPosition != null && initialPosition > 0) {
            lastPosition = initialPosition
        }

        if (currentPlayer.currentPosition <= 0 && lastPosition > 0) {
            currentPlayer.seekTo(lastPosition)
        }

        if (isFirstStart || wasPlaying) {
            currentPlayer.playWhenReady = true
        }

        isFirstStart = false
    }

    fun stop() {
        isStarted = false
        wasPlaying = currentPlayer.playWhenReady && currentPlayer.playbackState == Player.STATE_READY
        lastPosition = currentPlayer.currentPosition

        // Casting continues in the background.
        localPlayer.playWhenReady = false
    }

    fun retry() {
        error = null

        when (currentPlayer) {
            localPlayer -> {
                localPlayer.prepare()

                if (lastPosition > 0) localPlayer.seekTo(lastPosition)
            }
            castPlayer -> castPlayer.setMediaItem(media.toCastMediaItem(), lastPosition.coerceAtLeast(0))
        }
    }

    /**
     * Plays [newMedia] instead of the current one, e.g. when the user started another episode while this one played.
     */
    fun replace(newMedia: StreamMedia) {
        media = newMedia
        updateReferer()
        wasPlaying = false
        lastPosition = C.TIME_UNSET
        error = null

        when (currentPlayer) {
            castPlayer -> castPlayer.setMediaItem(media.toCastMediaItem())
            else -> localPlayer.setMediaItem(media.toMediaItem())
        }

        currentPlayer.prepare()
        currentPlayer.playWhenReady = true
    }

    fun release() {
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.removeListener(listener)
        castPlayer?.release()

        localPlayer.removeListener(listener)
        localPlayer.release()

        adsLoader?.release()
    }

    private fun switchTo(player: Player) {
        val previous = currentPlayer

        if (previous == player) return

        val position = previous.currentPosition

        previous.playWhenReady = false

        if (player == castPlayer) {
            castPlayer.setMediaItem(media.toCastMediaItem(), position)
            castPlayer.prepare()
        } else {
            player.seekTo(position)
        }

        player.playWhenReady = isStarted || player == castPlayer
        currentPlayer = player
    }

    private fun StreamMedia.toMediaItem() = MediaItem.Builder()
        .setUri(uri)
        .setMimeType(mimeType.toPlayerMimeType())
        .setMediaMetadata(metadata())
        .apply { adTag?.let { setAdsConfiguration(MediaItem.AdsConfiguration.Builder(it).build()) } }
        .build()

    // Cast receivers only support plain videos, which the streams that can be cast are.
    private fun StreamMedia.toCastMediaItem() = MediaItem.Builder()
        .setUri(uri)
        .setMimeType(MimeTypes.VIDEO_MP4)
        .setMediaMetadata(metadata())
        .build()

    private fun StreamMedia.metadata() = MediaMetadata.Builder()
        .setTitle(name)
        .setTrackNumber(episode)
        .setArtworkUri(coverUri)
        .setMediaType(MediaMetadata.MEDIA_TYPE_TV_SHOW)
        .build()

    private fun String?.toPlayerMimeType() = when (this?.lowercase()) {
        "application/x-mpegurl", "application/vnd.apple.mpegurl" -> MimeTypes.APPLICATION_M3U8
        "application/dash+xml" -> MimeTypes.APPLICATION_MPD
        null, "", "*/*", "video/*" -> null
        else -> this
    }

    private fun prepareLocalPlayer() {
        isPrepared = true

        localPlayer.setMediaItem(media.toMediaItem())
        localPlayer.prepare()
    }

    private fun updateReferer() {
        dataSourceFactory.setDefaultRequestProperties(media.referer?.let { mapOf("Referer" to it) } ?: emptyMap())
    }
}

/**
 * The stream to play.
 */
data class StreamMedia(
    val uri: Uri,
    val mimeType: String?,
    val name: String?,
    val episode: Int?,
    val coverUri: Uri?,
    val referer: String?,
    val adTag: Uri?
)
