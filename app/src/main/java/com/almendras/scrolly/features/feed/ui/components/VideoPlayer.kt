@file:OptIn(UnstableApi::class)
package com.almendras.scrolly.features.feed.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayer(
    uri: Uri,
    isVisible: Boolean,
    isPlaying: Boolean,
    zoomScale: Float,
    videoResizeMode: Int,
    playbackSpeed: Float,
    isMuted: Boolean,
    isUiVisible: Boolean,
    onToggleUi: () -> Unit,
    onTogglePlay: () -> Unit,
    onProgressUpdate: (Long, Long) -> Unit,
    seekToPosition: Long? = null,
    onSeekConsumed: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build()
            setAudioAttributes(audioAttributes, true)
        }
    }

    val scope = rememberCoroutineScope()
    var isHolding by remember { mutableStateOf(false) }
    var tempSpeed by remember { mutableFloatStateOf(1f) }

    // NUEVO: Estado para mostrar el cartel de salto (+10s o -10s)
    var doubleTapAction by remember { mutableStateOf("") }

    LaunchedEffect(uri) {
        exoPlayer.setMediaItem(MediaItem.fromUri(uri))
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying, isVisible) {
        if (isVisible && isPlaying) exoPlayer.play() else exoPlayer.pause()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                exoPlayer.pause()
            } else if (event == Lifecycle.Event.ON_RESUME && isVisible && isPlaying) {
                exoPlayer.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(seekToPosition) {
        seekToPosition?.let { exoPlayer.seekTo(it); onSeekConsumed() }
    }

    LaunchedEffect(isVisible, isPlaying) {
        if (isVisible && isPlaying) {
            while (true) {
                onProgressUpdate(exoPlayer.currentPosition, exoPlayer.duration)
                delay(700)
            }
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures(
                // NUEVO: Lógica del Doble Toque
                onDoubleTap = { offset ->
                    val screenCenter = size.width / 2
                    if (offset.x > screenCenter) {
                        // Mitad derecha: Adelantar 10 segundos (10,000 milisegundos)
                        exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                        doubleTapAction = "⏩ +10s"
                    } else {
                        // Mitad izquierda: Retroceder 10 segundos
                        exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                        doubleTapAction = "⏪ -10s"
                    }

                    // Vibración para confirmar el doble toque
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    // Ocultar el cartel después de un segundo
                    scope.launch {
                        delay(800)
                        doubleTapAction = ""
                    }
                },
                onPress = { offset ->
                    val job = scope.launch {
                        delay(400)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isHolding = true
                        tempSpeed = if (offset.x < size.width / 3) 0.5f else 2.0f
                    }
                    tryAwaitRelease()
                    job.cancel()
                    isHolding = false
                },
                onTap = { onToggleUi() }
            )
        }
        .pointerInput(Unit) {
            var totalDrag = 0f
            detectHorizontalDragGestures(
                onDragStart = { totalDrag = 0f },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    if (totalDrag < -100f) onSwipeLeft()
                    if (totalDrag > 100f) onSwipeRight()
                }
            )
        }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { view ->
                view.keepScreenOn = isVisible && isPlaying

                if (view.resizeMode != videoResizeMode) {
                    view.resizeMode = videoResizeMode
                }

                val targetVolume = if (isMuted) 0f else 1f
                if (view.player?.volume != targetVolume) {
                    view.player?.volume = targetVolume
                }

                val targetSpeed = if (isHolding) tempSpeed else playbackSpeed
                if (view.player?.playbackParameters?.speed != targetSpeed) {
                    view.player?.playbackParameters = PlaybackParameters(targetSpeed)
                }
            },
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
        )

        // ANIMACIÓN: Cartel central del Botón Play/Pausa
        AnimatedVisibility(
            visible = isUiVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onTogglePlay()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        // ANIMACIÓN: Cartel superior para 2x velocidad
        AnimatedVisibility(visible = isHolding, modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)) {
            Text(
                text = if (tempSpeed > 1f) "2.0 x ⏩" else "0.5 x ⏪",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier.background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp)).padding(8.dp)
            )
        }

        // NUEVO: ANIMACIÓN: Carteles laterales para los saltos de 10 segundos
        AnimatedVisibility(
            visible = doubleTapAction.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
            // Alineamos a la izquierda o derecha según donde se haya tocado
            modifier = Modifier
                .align(if (doubleTapAction.contains("+")) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 40.dp)
        ) {
            Text(
                text = doubleTapAction,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}