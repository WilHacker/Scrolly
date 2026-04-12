@file:OptIn(UnstableApi::class, ExperimentalFoundationApi::class)
package com.almendras.scrolly.features.feed.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.almendras.scrolly.features.feed.service.AudioService
import com.almendras.scrolly.features.feed.service.PlayerManager
import com.almendras.scrolly.features.feed.ui.components.VideoPlayer
import com.almendras.scrolly.features.feed.viewmodel.FeedViewModel

@Composable
fun FeedScreen(viewModel: FeedViewModel, initialPage: Int = 0, onNavigateBack: () -> Unit) {

    // Observamos los estados del ViewModel
    val videos by viewModel.videos.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)

    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember { PlayerManager.getPlayer(context) }

    val speedList = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f)
    val zoomList = listOf(1f, 1.25f, 1.5f, 2f, 3f)

    var isAudioOnly by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var videoResizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var isMuted by remember { mutableStateOf(false) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) { viewModel.loadVideos() }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, storagePermission) == PackageManager.PERMISSION_GRANTED) {
            viewModel.loadVideos()
        } else {
            storagePermissionLauncher.launch(storagePermission)
        }
    }

    DisposableEffect(lifecycleOwner, isAudioOnly) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && !isAudioOnly) exoPlayer.pause()
            else if (event == Lifecycle.Event.ON_RESUME && !isAudioOnly && exoPlayer.playWhenReady) exoPlayer.play()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val bottomBarPadding = if (isLandscape) 30.dp else 85.dp
    val infoPadding = if (isLandscape) 60.dp else 125.dp
    val sidePadding = if (isLandscape) 40.dp else 12.dp
    val topPadding = if (isLandscape) 20.dp else 48.dp

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var currentVolume by remember { mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var currentBrightness by remember { mutableFloatStateOf(0.5f) }

    val bwSliderColors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            ShimmerItem()
        } else if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron videos", color = Color.White, fontSize = 16.sp)
            }
        } else {
            val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { videos.size })

            VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 0) { page ->
                val video = videos[page]
                key(video.id) {
                    val isVisible = pagerState.currentPage == page
                    var isUiVisible by remember { mutableStateOf(false) }
                    var isPlaying by remember { mutableStateOf(true) }
                    var isLeftPanelOpen by remember { mutableStateOf(false) }
                    var isRightPanelOpen by remember { mutableStateOf(false) }
                    var showSpeedList by remember { mutableStateOf(false) }
                    var showZoomList by remember { mutableStateOf(false) }
                    var currentPos by remember { mutableLongStateOf(0L) }
                    var totalDuration by remember { mutableLongStateOf(1L) }
                    var seekTo by remember { mutableLongStateOf(-1L) }
                    var isDraggingSlider by remember { mutableStateOf(false) }

                    LaunchedEffect(isPlaying, isVisible) {
                        if (isVisible && isPlaying) {
                            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayer(
                            uri = video.uri,
                            isVisible = isVisible,
                            isPlaying = isPlaying,
                            zoomScale = zoomScale,
                            videoResizeMode = videoResizeMode,
                            playbackSpeed = playbackSpeed,
                            isMuted = isMuted,
                            isUiVisible = isUiVisible,
                            onProgressUpdate = { pos, dur -> if (!isDraggingSlider) { currentPos = pos; totalDuration = dur } },
                            seekToPosition = if (seekTo != -1L) seekTo else null,
                            onSeekConsumed = { seekTo = -1L },
                            onToggleUi = {
                                isUiVisible = !isUiVisible
                                if (!isUiVisible) { isLeftPanelOpen = false; isRightPanelOpen = false; showSpeedList = false; showZoomList = false }
                            },
                            onTogglePlay = { isPlaying = !isPlaying },
                            onSwipeLeft = { if (isLeftPanelOpen) isLeftPanelOpen = false else { isRightPanelOpen = true; isUiVisible = true } },
                            onSwipeRight = { if (isRightPanelOpen) isRightPanelOpen = false else { isLeftPanelOpen = true; isUiVisible = true } }
                        )

                        AnimatedVisibility(visible = isUiVisible, enter = fadeIn(), exit = fadeOut()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                IconButton(onClick = onNavigateBack, modifier = Modifier.padding(top = topPadding, start = 16.dp).background(Color.Black.copy(0.4f), CircleShape)) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                                }

                                Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = sidePadding)) {
                                    if (isLeftPanelOpen) {
                                        Column(modifier = Modifier.background(Color.Black.copy(0.7f), RoundedCornerShape(24.dp)).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Filled.WbSunny, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                            Slider(value = currentBrightness, onValueChange = { currentBrightness = it; activity?.window?.let { w -> val p = w.attributes; p.screenBrightness = it; w.attributes = p } }, modifier = Modifier.width(130.dp), colors = bwSliderColors)
                                            Spacer(modifier = Modifier.height(35.dp))
                                            Icon(if(currentVolume == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                            Slider(value = currentVolume.toFloat(), valueRange = 0f..maxVolume.toFloat(), onValueChange = { currentVolume = it.toInt(); audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0) }, modifier = Modifier.width(130.dp), colors = bwSliderColors)
                                            IconButton(onClick = { isLeftPanelOpen = false }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White) }
                                        }
                                    } else {
                                        IconButton(onClick = { isLeftPanelOpen = true; isRightPanelOpen = false }, modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)) {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
                                        }
                                    }
                                }

                                Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = sidePadding).widthIn(max = 220.dp)) {
                                    if (isRightPanelOpen) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AnimatedVisibility(visible = showSpeedList || showZoomList, enter = slideInHorizontally(initialOffsetX = { it }), exit = slideOutHorizontally(targetOffsetX = { it })) {
                                                Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.85f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(end = 8.dp).height(280.dp).width(70.dp)) {
                                                    LazyColumn(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 8.dp)) {
                                                        if (showSpeedList) {
                                                            items(speedList) { speed ->
                                                                TextButton(onClick = { playbackSpeed = speed; showSpeedList = false }) {
                                                                    Text("${speed}x", color = if(playbackSpeed == speed) Color.White else Color.Gray, fontSize = 13.sp, fontWeight = if(playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal)
                                                                }
                                                            }
                                                        } else if (showZoomList) {
                                                            items(zoomList) { zoom ->
                                                                TextButton(onClick = { zoomScale = zoom; showZoomList = false }) {
                                                                    Text("${(zoom * 100).toInt()}%", color = if(zoomScale == zoom) Color.White else Color.Gray, fontSize = 13.sp, fontWeight = if(zoomScale == zoom) FontWeight.Bold else FontWeight.Normal)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            Column(modifier = Modifier.background(Color.Black.copy(0.7f), RoundedCornerShape(24.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                IconButton(onClick = { isRightPanelOpen = false; showSpeedList = false; showZoomList = false }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White) }
                                                IconButton(onClick = { viewModel.toggleFavorite(video) }) { Icon(if (video.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if(video.isFavorite) Color.White else Color.White.copy(0.5f)) }
                                                IconButton(onClick = { isMuted = !isMuted }) { Icon(if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White) }
                                                IconButton(onClick = { showSpeedList = !showSpeedList; showZoomList = false }) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Filled.Speed, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                        Text("${playbackSpeed}x", color = Color.White, fontSize = 9.sp)
                                                    }
                                                }
                                                IconButton(onClick = { showZoomList = !showZoomList; showSpeedList = false }) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Filled.ZoomIn, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                        Text("${(zoomScale * 100).toInt()}%", color = Color.White, fontSize = 9.sp)
                                                    }
                                                }
                                                IconButton(onClick = { videoResizeMode = if (videoResizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT }) {
                                                    Icon(if (videoResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, null, tint = Color.White)
                                                }
                                                IconButton(onClick = {
                                                    isAudioOnly = !isAudioOnly
                                                    val intent = Intent(context, AudioService::class.java)
                                                    if (isAudioOnly) context.startService(intent) else context.stopService(intent)
                                                }) { Icon(Icons.Filled.Headset, null, tint = if(isAudioOnly) Color.White else Color.White.copy(0.5f)) }
                                                IconButton(onClick = { activity?.enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().build()) }) { Icon(Icons.Filled.PictureInPicture, null, tint = Color.White) }
                                            }
                                        }
                                    } else {
                                        IconButton(onClick = { isRightPanelOpen = true; isLeftPanelOpen = false }, modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)) {
                                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
                                        }
                                    }
                                }

                                Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = infoPadding)) {
                                    Text(video.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(video.folderName, color = Color.LightGray, fontSize = 11.sp)
                                }

                                Slider(
                                    value = currentPos.toFloat(),
                                    valueRange = 0f..totalDuration.coerceAtLeast(1L).toFloat(),
                                    onValueChange = { isDraggingSlider = true; currentPos = it.toLong() },
                                    onValueChangeFinished = { isDraggingSlider = false; seekTo = currentPos },
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(start = 24.dp, end = 24.dp, bottom = bottomBarPadding),
                                    colors = bwSliderColors
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShimmerItem() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer_anim"
    )

    val shimmerColors = listOf(
        Color.DarkGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.3f),
        Color.DarkGray.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Spacer(modifier = Modifier.fillMaxSize().background(brush))
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp).padding(bottom = 120.dp)) {
            Box(modifier = Modifier.size(width = 150.dp, height = 20.dp).background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.size(width = 100.dp, height = 15.dp).background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp)))
        }
    }
}