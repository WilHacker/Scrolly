@file:OptIn(UnstableApi::class)

package com.almendras.scrolly.features.feed.ui.feed

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.almendras.scrolly.R
import com.almendras.scrolly.core.player.AudioService
import com.almendras.scrolly.core.player.PlayerManager
import com.almendras.scrolly.core.player.PlayerPool
import com.almendras.scrolly.features.feed.domain.model.VideoFilterType
import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.model.applyFilter
import com.almendras.scrolly.features.feed.ui.FeedViewModel
import com.almendras.scrolly.features.feed.ui.feed.components.LeftControlPanel
import com.almendras.scrolly.features.feed.ui.feed.components.RightControlPanel
import com.almendras.scrolly.features.feed.ui.feed.components.ShimmerItem
import com.almendras.scrolly.features.feed.ui.feed.components.VideoPlayer
import kotlinx.coroutines.delay

/** Preferencias de reproducción compartidas entre todas las páginas del feed. */
@Immutable
data class PlaybackPrefs(
    val isPlaying: Boolean,
    val speed: Float,
    val zoom: Float,
    val resizeMode: Int,
    val isMuted: Boolean,
    val isAudioOnly: Boolean
)

@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    filterType: VideoFilterType,
    filterValue: String,
    initialPage: Int = 0,
    onNavigateBack: () -> Unit
) {
    val allVideos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // Congelamos los IDs del feed al entrar: si quitas un favorito dentro del
    // feed de Favoritos, el video no desaparece bajo tus dedos. El estado del
    // corazón sí se mantiene en vivo porque se re-mapea desde allVideos.
    var feedIds by remember { mutableStateOf<List<Long>?>(null) }
    LaunchedEffect(allVideos) {
        if (feedIds == null && allVideos.isNotEmpty()) {
            feedIds = allVideos.applyFilter(filterType, filterValue).map { it.id }
        }
    }
    val videos = remember(allVideos, feedIds) {
        val ids = feedIds
        if (ids == null) {
            emptyList()
        } else {
            val byId = allVideos.associateBy { it.id }
            ids.mapNotNull { byId[it] }
        }
    }

    val playerPool = remember { PlayerPool(context) }

    DisposableEffect(Unit) {
        onDispose {
            context.stopService(Intent(context, AudioService::class.java))
            PlayerManager.activePlayer = null
            playerPool.releaseAll()
        }
    }

    // Permiso de notificaciones (para la notificación de la sesión de medios)
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            isLoading && videos.isEmpty() -> ShimmerItem()

            videos.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.feed_empty), color = Color.White, fontSize = 16.sp)
            }

            else -> FeedPager(
                videos = videos,
                initialPage = initialPage,
                playerPool = playerPool,
                onToggleFavorite = viewModel::toggleFavorite,
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
private fun FeedPager(
    videos: List<VideoItem>,
    initialPage: Int,
    playerPool: PlayerPool,
    onToggleFavorite: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (videos.size - 1).coerceAtLeast(0)),
        pageCount = { videos.size }
    )

    var isPlaying by remember { mutableStateOf(true) }
    var isAudioOnly by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var videoResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isMuted by remember { mutableStateOf(false) }

    // Brillo y volumen (compartidos entre páginas)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember {
        mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }

    // Al cruzar a una página nueva (a mitad de la animación, como TikTok):
    // reproducir y registrar el player activo
    LaunchedEffect(pagerState, playerPool) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            isPlaying = true
            PlayerManager.activePlayer = playerPool.playerFor(page)
        }
    }

    // Pausar/reanudar con el ciclo de vida (salvo en modo solo-audio)
    DisposableEffect(lifecycleOwner, isAudioOnly) {
        val observer = LifecycleEventObserver { _, event ->
            if (!isAudioOnly) {
                val player = playerPool.playerFor(pagerState.currentPage)
                if (event == Lifecycle.Event.ON_PAUSE) player?.pause()
                else if (event == Lifecycle.Event.ON_RESUME && isPlaying) player?.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val prefs = PlaybackPrefs(
        isPlaying = isPlaying,
        speed = playbackSpeed,
        zoom = zoomScale,
        resizeMode = videoResizeMode,
        isMuted = isMuted,
        isAudioOnly = isAudioOnly
    )

    VerticalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // 1 página extra arriba y abajo ya compuesta y con el video preparado:
        // el swipe encuentra el primer frame listo (clave de la fluidez)
        beyondViewportPageCount = 1,
        key = { videos[it].id },
        // Snap estilo TikTok: animación rígida y rápida, y basta arrastrar ~20%
        // de la pantalla para comprometerse con la página siguiente
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            snapPositionalThreshold = 0.2f
        )
    ) { page ->
        val video = videos[page]

        FeedPage(
            video = video,
            page = page,
            pagerState = pagerState,
            playerPool = playerPool,
            prefs = prefs,
            onTogglePlay = { isPlaying = !isPlaying },
            onSpeedSelected = { playbackSpeed = it },
            onZoomSelected = { zoomScale = it },
            onToggleResizeMode = {
                videoResizeMode = if (videoResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            onToggleMute = { isMuted = !isMuted },
            onToggleAudioOnly = {
                isAudioOnly = !isAudioOnly
                val intent = Intent(context, AudioService::class.java)
                if (isAudioOnly) {
                    PlayerManager.activePlayer = playerPool.playerFor(pagerState.currentPage)
                    context.startService(intent)
                } else {
                    context.stopService(intent)
                }
            },
            onEnterPip = {
                activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            },
            brightness = currentBrightness,
            onBrightnessChange = { value ->
                currentBrightness = value
                activity?.window?.let { window ->
                    val params = window.attributes
                    params.screenBrightness = value
                    window.attributes = params
                }
            },
            volume = currentVolume,
            maxVolume = maxVolume,
            onVolumeChange = { value ->
                currentVolume = value
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
            },
            onToggleFavorite = { onToggleFavorite(video) },
            onNavigateBack = onNavigateBack
        )
    }
}

@Composable
private fun FeedPage(
    video: VideoItem,
    page: Int,
    pagerState: PagerState,
    playerPool: PlayerPool,
    prefs: PlaybackPrefs,
    onTogglePlay: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onZoomSelected: (Float) -> Unit,
    onToggleResizeMode: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleAudioOnly: () -> Unit,
    onEnterPip: () -> Unit,
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    volume: Int,
    maxVolume: Int,
    onVolumeChange: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // El player viene del pool: se recicla entre páginas, no se recrea
    val player = remember(page) { playerPool.acquire(page) }
    DisposableEffect(page) {
        onDispose { playerPool.recycle(page) }
    }

    // Preparar el media (también en páginas adyacentes aún no visibles = precarga)
    LaunchedEffect(player, video.uri) {
        val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
        if (currentUri != video.uri) {
            player.setMediaItem(MediaItem.fromUri(video.uri))
            player.prepare()
        }
    }

    val isActive = pagerState.currentPage == page

    // Solo la página actual reproduce; las adyacentes quedan preparadas en pausa
    LaunchedEffect(isActive, prefs.isPlaying) {
        if (isActive && prefs.isPlaying) player.play() else player.pause()
    }

    // Si el formato/códec no se puede reproducir, avisar en vez de quedar en negro
    var playbackError by remember(video.uri) { mutableStateOf(false) }
    DisposableEffect(player, video.uri) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    var isUiVisible by remember { mutableStateOf(false) }
    var isLeftPanelOpen by remember { mutableStateOf(false) }
    var isRightPanelOpen by remember { mutableStateOf(false) }
    var currentPos by remember { mutableLongStateOf(0L) }
    var totalDuration by remember { mutableLongStateOf(1L) }
    var isDraggingSlider by remember { mutableStateOf(false) }

    // El progreso solo se sondea con la UI visible: cero recomposiciones durante
    // la reproducción normal (clave para que el scroll no compita con la UI)
    LaunchedEffect(isUiVisible, isActive) {
        while (isUiVisible && isActive) {
            if (!isDraggingSlider) {
                currentPos = player.currentPosition
                totalDuration = player.duration.coerceAtLeast(1L)
            }
            delay(PlaybackConfig.PROGRESS_UPDATE_MS)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bottomBarPadding = if (isLandscape) 30.dp else 85.dp
    val infoPadding = if (isLandscape) 60.dp else 125.dp
    val sidePadding = if (isLandscape) 40.dp else 12.dp
    val topPadding = if (isLandscape) 20.dp else 48.dp

    val bwSliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        VideoPlayer(
            player = player,
            isPlaying = prefs.isPlaying,
            keepScreenOn = isActive && prefs.isPlaying,
            zoomScale = prefs.zoom,
            videoResizeMode = prefs.resizeMode,
            playbackSpeed = prefs.speed,
            isMuted = prefs.isMuted,
            isUiVisible = isUiVisible,
            onToggleUi = {
                isUiVisible = !isUiVisible
                if (!isUiVisible) {
                    isLeftPanelOpen = false
                    isRightPanelOpen = false
                }
            },
            onTogglePlay = onTogglePlay,
            onSwipeLeft = {
                if (isLeftPanelOpen) isLeftPanelOpen = false
                else { isRightPanelOpen = true; isUiVisible = true }
            },
            onSwipeRight = {
                if (isRightPanelOpen) isRightPanelOpen = false
                else { isLeftPanelOpen = true; isUiVisible = true }
            }
        )

        if (playbackError) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.feed_video_error),
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }

        AnimatedVisibility(visible = isUiVisible, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .padding(top = topPadding, start = 16.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = Color.White
                    )
                }

                // Panel izquierdo (brillo / volumen)
                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = sidePadding)) {
                    if (isLeftPanelOpen) {
                        LeftControlPanel(
                            brightness = brightness,
                            onBrightnessChange = onBrightnessChange,
                            volume = volume,
                            maxVolume = maxVolume,
                            onVolumeChange = onVolumeChange,
                            sliderColors = bwSliderColors,
                            onClose = { isLeftPanelOpen = false }
                        )
                    } else {
                        IconButton(
                            onClick = { isLeftPanelOpen = true; isRightPanelOpen = false },
                            modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
                        }
                    }
                }

                // Panel derecho (favorito, velocidad, zoom, etc.)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = sidePadding)
                        .widthIn(max = 220.dp)
                ) {
                    if (isRightPanelOpen) {
                        RightControlPanel(
                            isFavorite = video.isFavorite,
                            onToggleFavorite = onToggleFavorite,
                            isMuted = prefs.isMuted,
                            onToggleMute = onToggleMute,
                            playbackSpeed = prefs.speed,
                            onSpeedSelected = onSpeedSelected,
                            zoomScale = prefs.zoom,
                            onZoomSelected = onZoomSelected,
                            videoResizeMode = prefs.resizeMode,
                            onToggleResizeMode = onToggleResizeMode,
                            isAudioOnly = prefs.isAudioOnly,
                            onToggleAudioOnly = onToggleAudioOnly,
                            onEnterPip = onEnterPip,
                            onClose = { isRightPanelOpen = false }
                        )
                    } else {
                        IconButton(
                            onClick = { isRightPanelOpen = true; isLeftPanelOpen = false },
                            modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
                        }
                    }
                }

                // Nombre y carpeta del video
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = infoPadding)
                ) {
                    Text(video.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(video.folderName, color = Color.LightGray, fontSize = 11.sp)
                }

                // Barra de progreso
                Slider(
                    value = currentPos.toFloat(),
                    valueRange = 0f..totalDuration.coerceAtLeast(1L).toFloat(),
                    onValueChange = {
                        isDraggingSlider = true
                        currentPos = it.toLong()
                    },
                    onValueChangeFinished = {
                        isDraggingSlider = false
                        player.seekTo(currentPos)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 24.dp, end = 24.dp, bottom = bottomBarPadding),
                    colors = bwSliderColors
                )
            }
        }
    }
}
