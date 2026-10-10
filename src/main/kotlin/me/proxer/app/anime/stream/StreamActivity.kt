package me.proxer.app.anime.stream

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Rational
import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.getSystemService
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.ImageRequest
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.delay
import me.proxer.app.R
import me.proxer.app.anime.resolver.StreamResolutionResult
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.AD_TAG_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.COVER_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.EPISODE_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.ID_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.INTERNAL_PLAYER_ONLY_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.LANGUAGE_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.NAME_EXTRA
import me.proxer.app.anime.resolver.StreamResolutionResult.Video.Companion.REFERER_EXTRA
import me.proxer.app.base.ComposeActivity
import me.proxer.app.ui.components.DialogButton
import me.proxer.app.util.extension.getSafeStringExtra
import me.proxer.app.util.extension.newTask
import me.proxer.app.util.extension.toEpisodeAppString
import me.proxer.app.util.extension.toPrefixedUrlOrNull
import me.proxer.app.util.extension.toast
import me.proxer.library.enums.AnimeLanguage
import me.proxer.library.enums.Category
import me.proxer.library.util.ProxerUrls.hasProxerStreamFileHost
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import timber.log.Timber
import kotlin.math.roundToInt

/**
 * The player for streams. Supports Google Cast for the streams of Proxer, picture-in-picture and gestures for seeking,
 * the volume and the brightness.
 *
 * @author Ruben Gees
 */
@OptIn(UnstableApi::class)
class StreamActivity : ComposeActivity() {

    private companion object {
        private const val PREVIEW_MIME_TYPE = "video/mp4"
        private const val CLOUDFLARE_HOST = "videodelivery.net"
        private const val CONTROLS_TIMEOUT_MS = 3_000L
        private const val INDICATOR_TIMEOUT_MS = 1_000L
        private const val SEEK_STEP_SECONDS = 10
        private const val SWIPE_SENSITIVITY = 1.5f
        private const val MIN_BRIGHTNESS = 0.01f
        private const val MAX_SYSTEM_BRIGHTNESS = 255f
    }

    override val hasDarkSystemBars = true

    private val id: String
        get() = intent.getSafeStringExtra(ID_EXTRA)

    private val name: String
        get() = intent.getSafeStringExtra(NAME_EXTRA)

    private val episode: Int
        get() = intent.getIntExtra(EPISODE_EXTRA, -1).let { if (it <= 0) 1 else it }

    private val language: AnimeLanguage
        get() = intent.getSerializableExtra(LANGUAGE_EXTRA) as? AnimeLanguage ?: AnimeLanguage.ENGLISH_SUB

    private val uri: Uri
        get() = requireNotNull(intent.data)

    private val mimeType: String?
        get() = intent.type

    private val isProxerStream: Boolean
        get() {
            val url = intent.dataString?.toPrefixedUrlOrNull()

            return url != null && (url.hasProxerStreamFileHost || url.host == CLOUDFLARE_HOST)
        }

    private val isInternalPlayerOnly: Boolean
        get() = intent.getBooleanExtra(INTERNAL_PLAYER_ONLY_EXTRA, false)

    private val client by inject<OkHttpClient>()

    private lateinit var playerManager: StreamPlayerManager

    private var isInPictureInPicture by mutableStateOf(false)

    private val pictureInPictureListener = Consumer<PictureInPictureModeChangedInfo> {
        isInPictureInPicture = it.isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        playerManager = StreamPlayerManager(this, client, if (isProxerStream) getSafeCastContext() else null, media())

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        addOnPictureInPictureModeChangedListener(pictureInPictureListener)

        if (savedInstanceState == null) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    override fun onStart() {
        super.onStart()

        playerManager.start(storageHelper.getLastAnimePosition(id, episode, language))
    }

    override fun onStop() {
        playerManager.stop()

        val lastPosition = playerManager.currentPlayer.currentPosition

        if (lastPosition > 0) {
            storageHelper.putLastAnimePosition(id, episode, language, lastPosition)
        }

        super.onStop()
    }

    override fun onDestroy() {
        removeOnPictureInPictureModeChangedListener(pictureInPictureListener)
        playerManager.release()

        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (intent.data != null && intent.data != this.intent.data) {
            this.intent = intent

            playerManager.replace(media())
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()

        if (playerManager.currentPlayer.isPlaying && playerManager.currentPlayer !is CastPlayer) {
            enterPictureInPicture()
        }
    }

    @Composable
    override fun Content() {
        val player = playerManager.currentPlayer
        val state = rememberPlayerUiState(player)
        val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isCasting = player is CastPlayer
        val error = playerManager.error

        var areControlsVisible by rememberSaveable { mutableStateOf(true) }
        var isSeeking by remember { mutableStateOf(false) }
        var seekIndicator by remember { mutableStateOf<Pair<Boolean, Int>?>(null) }
        var swipeTarget by remember { mutableStateOf<SwipeTarget?>(null) }
        var swipeLevel by remember { mutableFloatStateOf(0f) }
        var swipeEnds by remember { mutableIntStateOf(0) }

        val previewLoader = remember(uri) {
            if (mimeType == PREVIEW_MIME_TYPE) {
                PreviewLoader(uri, intent.getStringExtra(REFERER_EXTRA), isProxerStream)
            } else {
                null
            }
        }

        DisposableEffect(previewLoader) {
            onDispose { previewLoader?.release() }
        }

        LaunchedEffect(previewLoader) { previewLoader?.prepare() }

        val showControls = (areControlsVisible || isCasting) && !state.isPlayingAd && !isInPictureInPicture

        LaunchedEffect(areControlsVisible, state.isPlaying, isSeeking, isCasting) {
            if (areControlsVisible && state.isPlaying && !isSeeking && !isCasting) {
                delay(CONTROLS_TIMEOUT_MS)

                areControlsVisible = false
            }
        }

        LaunchedEffect(showControls) { setSystemBarsVisible(showControls) }
        LaunchedEffect(state.isPlaying, state.isLoading) { setKeepScreenOn(state.isPlaying || state.isLoading) }

        LaunchedEffect(seekIndicator) {
            if (seekIndicator != null) {
                delay(INDICATOR_TIMEOUT_MS)

                seekIndicator = null
            }
        }

        LaunchedEffect(swipeEnds) {
            if (swipeEnds > 0) {
                delay(INDICATOR_TIMEOUT_MS)

                swipeTarget = null
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)

                        playerManager.adViewGroup = adViewGroup

                        loadArtwork(this)
                    }
                },
                update = { it.player = player },
                onRelease = { it.player = null },
                modifier = Modifier.fillMaxSize()
            )

            // The ads have their own controls, which must not be covered.
            if (!state.isPlayingAd) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .playerGestures(
                            onTap = { areControlsVisible = !areControlsVisible },
                            onDoubleTap = { isForward ->
                                if (isForward) state.fastForward() else state.rewind()

                                val previous = seekIndicator?.takeIf { it.first == isForward }?.second ?: 0

                                seekIndicator = isForward to previous + SEEK_STEP_SECONDS
                            },
                            onSwipe = { target, delta ->
                                if (swipeTarget != target) swipeLevel = currentLevel(target)

                                swipeTarget = target
                                swipeLevel = applyLevel(target, swipeLevel + delta * SWIPE_SENSITIVITY)
                            },
                            onSwipeEnd = { swipeEnds++ }
                        )
                )
            }

            SeekIndicator(
                seconds = seekIndicator?.takeIf { !it.first }?.second,
                isForward = false,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 48.dp)
            )

            SeekIndicator(
                seconds = seekIndicator?.takeIf { it.first }?.second,
                isForward = true,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 48.dp)
            )

            LevelIndicator(swipeTarget, swipeLevel, Modifier.align(Alignment.Center))

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                StreamControls(
                    state = state,
                    title = name,
                    subtitle = Category.ANIME.toEpisodeAppString(this@StreamActivity, episode),
                    isLandscape = isLandscape,
                    onNavigateUp = ::finish,
                    onToggleOrientation = ::toggleOrientation,
                    onSeekingChange = { isSeeking = it },
                    loadPreview = previewLoader?.let { loader -> loader::frameAt },
                    actions = { ControlActions() }
                )
            }

            if (state.isLoading && !showControls && !state.isPlayingAd) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                )
            }
        }

        if (error != null) {
            AlertDialog(
                onDismissRequest = ::finish,
                text = { Text(stringResource(error.message)) },
                confirmButton = { DialogButton(R.string.error_action_retry, onClick = playerManager::retry) },
                dismissButton = { DialogButton(R.string.error_action_finish, onClick = ::finish) }
            )
        }
    }

    @Composable
    private fun ControlActions() {
        if (isProxerStream && getSafeCastContext() != null) {
            AndroidView(
                factory = { context ->
                    MediaRouteButton(ContextThemeWrapper(context, R.style.ThemeOverlay_App_OnImage)).also {
                        CastButtonFactory.setUpMediaRouteButton(context, it)
                    }
                },
                modifier = Modifier.size(48.dp)
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            IconButton(onClick = ::enterPictureInPicture, colors = whiteIconButtonColors()) {
                Icon(
                    painterResource(R.drawable.ic_symbol_picture_in_picture_alt),
                    contentDescription = stringResource(R.string.exoplayer_picture_in_picture_description)
                )
            }
        }

        StreamOverflowMenu(
            if (isInternalPlayerOnly) {
                emptyList()
            } else {
                listOf(stringResource(R.string.action_open_in_other_app) to ::openInOtherApp)
            }
        )
    }

    private fun media() = StreamMedia(
        uri = uri,
        mimeType = mimeType,
        name = intent.getStringExtra(NAME_EXTRA),
        episode = episode,
        coverUri = intent.getParcelableExtra(COVER_EXTRA),
        referer = intent.getStringExtra(REFERER_EXTRA),
        adTag = intent.getParcelableExtra(AD_TAG_EXTRA)
    )

    private fun loadArtwork(playerView: PlayerView) {
        val coverUri = intent.getParcelableExtra<Uri>(COVER_EXTRA) ?: return

        SingletonImageLoader.get(this).enqueue(
            ImageRequest.Builder(this)
                .data(coverUri)
                .target(onSuccess = { playerView.defaultArtwork = it.asDrawable(resources) })
                .build()
        )
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun toggleOrientation() {
        requestedOrientation = when (requestedOrientation) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    private fun setSystemBarsVisible(isVisible: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        if (isVisible || isInMultiWindowMode) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun setKeepScreenOn(keepScreenOn: Boolean) {
        if (keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9)).build())
            } catch (error: IllegalStateException) {
                Timber.w(error)
            }
        }
    }

    /**
     * The current volume or brightness from 0 to 1.
     */
    private fun currentLevel(target: SwipeTarget): Float = when (target) {
        SwipeTarget.VOLUME -> getSystemService<AudioManager>()?.let {
            it.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        } ?: 0f
        SwipeTarget.BRIGHTNESS -> window.attributes.screenBrightness.takeIf { it >= 0 }
            ?: (Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / MAX_SYSTEM_BRIGHTNESS)
    }

    /**
     * Sets the volume or brightness to [level] (from 0 to 1) and returns the level which was set.
     */
    private fun applyLevel(target: SwipeTarget, level: Float): Float {
        val safeLevel = level.coerceIn(0f, 1f)

        when (target) {
            SwipeTarget.VOLUME -> getSystemService<AudioManager>()?.let {
                val maxVolume = it.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

                if (!it.isVolumeFixed) {
                    it.setStreamVolume(AudioManager.STREAM_MUSIC, (safeLevel * maxVolume).roundToInt(), 0)
                }
            }
            SwipeTarget.BRIGHTNESS -> window.attributes = window.attributes.apply {
                screenBrightness = safeLevel.coerceAtLeast(MIN_BRIGHTNESS)
            }
        }

        return safeLevel
    }

    private fun openInOtherApp() {
        try {
            val referer = intent.getStringExtra(REFERER_EXTRA)
            val intent = StreamResolutionResult.Video(uri.toString().toHttpUrl(), mimeType ?: "video/*", referer)
                .makeIntent(this)
                .newTask()

            startActivity(intent)
            finish()
        } catch (ignored: ActivityNotFoundException) {
            toast(R.string.activity_stream_open_no_app)
        }
    }

    @Suppress("SwallowedException")
    private fun getSafeCastContext(): CastContext? {
        val availabilityResult = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        return if (availabilityResult == ConnectionResult.SUCCESS) {
            try {
                CastContext.getSharedInstance(this)
            } catch (error: Exception) {
                Timber.e(error)
                null
            }
        } else {
            null
        }
    }
}
