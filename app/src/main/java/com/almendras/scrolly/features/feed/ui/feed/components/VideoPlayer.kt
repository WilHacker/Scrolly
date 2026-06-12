@file:OptIn(UnstableApi::class)

package com.almendras.scrolly.features.feed.ui.feed.components

import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.almendras.scrolly.R
import com.almendras.scrolly.features.feed.ui.feed.PlaybackConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Superficie de video del feed. NO crea ni destruye el ExoPlayer: lo recibe del
 * [com.almendras.scrolly.core.player.PlayerPool], que recicla instancias entre
 * páginas para que el swipe sea fluido.
 */
@Composable
fun VideoPlayer(
    player: ExoPlayer,
    isPlaying: Boolean,
    keepScreenOn: Boolean,
    zoomScale: Float,
    videoResizeMode: Int,
    playbackSpeed: Float,
    isMuted: Boolean,
    isUiVisible: Boolean,
    onToggleUi: () -> Unit,
    onTogglePlay: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var isHolding by remember { mutableStateOf(false) }
    var tempSpeed by remember { mutableFloatStateOf(1f) }
    var doubleTapAction by remember { mutableStateOf("") }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { offset ->
                    val screenCenter = size.width / 2
                    if (offset.x > screenCenter) {
                        player.seekTo(player.currentPosition + PlaybackConfig.DOUBLE_TAP_SEEK_MS)
                        doubleTapAction = "⏩ +10s"
                    } else {
                        player.seekTo(
                            (player.currentPosition - PlaybackConfig.DOUBLE_TAP_SEEK_MS)
                                .coerceAtLeast(0L)
                        )
                        doubleTapAction = "⏪ -10s"
                    }
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch {
                        delay(800)
                        doubleTapAction = ""
                    }
                },
                onPress = { offset ->
                    // Mantener presionado: velocidad temporal (izquierda 0.5x, resto 2x)
                    val job = scope.launch {
                        delay(PlaybackConfig.HOLD_TO_SPEED_DELAY_MS)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isHolding = true
                        tempSpeed = if (offset.x < size.width / 3) {
                            PlaybackConfig.HOLD_SPEED_SLOW
                        } else {
                            PlaybackConfig.HOLD_SPEED_FAST
                        }
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
                    if (totalDrag < -PlaybackConfig.SWIPE_PANEL_THRESHOLD_PX) onSwipeLeft()
                    if (totalDrag > PlaybackConfig.SWIPE_PANEL_THRESHOLD_PX) onSwipeRight()
                }
            )
        }
    ) {
        AndroidView(
            factory = { ctx ->
                val view = LayoutInflater.from(ctx)
                    .inflate(R.layout.feed_player_view, null) as PlayerView
                view.apply {
                    this.player = player
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { view ->
                if (view.player !== player) view.player = player

                view.keepScreenOn = keepScreenOn

                if (view.resizeMode != videoResizeMode) {
                    view.resizeMode = videoResizeMode
                }

                val targetVolume = if (isMuted) 0f else 1f
                if (player.volume != targetVolume) {
                    player.volume = targetVolume
                }

                val targetSpeed = if (isHolding) tempSpeed else playbackSpeed
                if (player.playbackParameters.speed != targetSpeed) {
                    player.playbackParameters = PlaybackParameters(targetSpeed)
                }
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale)
        )

        // Botón central Play/Pausa
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

        // Cartel superior de velocidad temporal (mantener presionado)
        AnimatedVisibility(
            visible = isHolding,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            Text(
                text = if (tempSpeed > 1f) "2.0 x ⏩" else "0.5 x ⏪",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                modifier = Modifier
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            )
        }

        // Carteles laterales de salto de 10 segundos
        AnimatedVisibility(
            visible = doubleTapAction.isNotEmpty(),
            enter = fadeIn() + scaleIn(initialScale = 0.5f),
            exit = fadeOut() + scaleOut(targetScale = 0.5f),
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
