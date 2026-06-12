@file:OptIn(UnstableApi::class)

package com.almendras.scrolly.features.feed.ui.feed.components

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.almendras.scrolly.features.feed.ui.feed.PlaybackConfig

/** Panel izquierdo: brillo y volumen. */
@Composable
fun LeftControlPanel(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    volume: Int,
    maxVolume: Int,
    onVolumeChange: (Int) -> Unit,
    sliderColors: SliderColors,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(Color.Black.copy(0.7f), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.WbSunny, null, tint = Color.White, modifier = Modifier.size(22.dp))
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            modifier = Modifier.width(130.dp),
            colors = sliderColors
        )
        Spacer(modifier = Modifier.height(35.dp))
        Icon(
            if (volume == 0) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            null, tint = Color.White, modifier = Modifier.size(22.dp)
        )
        Slider(
            value = volume.toFloat(),
            valueRange = 0f..maxVolume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            modifier = Modifier.width(130.dp),
            colors = sliderColors
        )
        IconButton(onClick = onClose) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White)
        }
    }
}

/** Panel derecho: favorito, mute, velocidad, zoom, ajuste, audio en background y PiP. */
@Composable
fun RightControlPanel(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    playbackSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    zoomScale: Float,
    onZoomSelected: (Float) -> Unit,
    videoResizeMode: Int,
    onToggleResizeMode: () -> Unit,
    isAudioOnly: Boolean,
    onToggleAudioOnly: () -> Unit,
    onEnterPip: () -> Unit,
    onClose: () -> Unit
) {
    var showSpeedList by remember { mutableStateOf(false) }
    var showZoomList by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(
            visible = showSpeedList || showZoomList,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            SpeedZoomSelector(
                showSpeedList = showSpeedList,
                playbackSpeed = playbackSpeed,
                onSpeedSelected = { onSpeedSelected(it); showSpeedList = false },
                zoomScale = zoomScale,
                onZoomSelected = { onZoomSelected(it); showZoomList = false }
            )
        }

        Column(
            modifier = Modifier
                .background(Color.Black.copy(0.7f), RoundedCornerShape(24.dp))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { showSpeedList = false; showZoomList = false; onClose() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White)
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    null,
                    tint = if (isFavorite) Color.White else Color.White.copy(0.5f)
                )
            }
            IconButton(onClick = onToggleMute) {
                Icon(
                    if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    null, tint = Color.White
                )
            }
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
            IconButton(onClick = onToggleResizeMode) {
                Icon(
                    if (videoResizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
                        Icons.Filled.FullscreenExit
                    } else {
                        Icons.Filled.Fullscreen
                    },
                    null, tint = Color.White
                )
            }
            IconButton(onClick = onToggleAudioOnly) {
                Icon(
                    Icons.Filled.Headset, null,
                    tint = if (isAudioOnly) Color.White else Color.White.copy(0.5f)
                )
            }
            IconButton(onClick = onEnterPip) {
                Icon(Icons.Filled.PictureInPicture, null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun SpeedZoomSelector(
    showSpeedList: Boolean,
    playbackSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    zoomScale: Float,
    onZoomSelected: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.85f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.padding(end = 8.dp).height(280.dp).width(70.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (showSpeedList) {
                items(PlaybackConfig.SPEED_OPTIONS) { speed ->
                    TextButton(onClick = { onSpeedSelected(speed) }) {
                        Text(
                            "${speed}x",
                            color = if (playbackSpeed == speed) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            } else {
                items(PlaybackConfig.ZOOM_OPTIONS) { zoom ->
                    TextButton(onClick = { onZoomSelected(zoom) }) {
                        Text(
                            "${(zoom * 100).toInt()}%",
                            color = if (zoomScale == zoom) Color.White else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = if (zoomScale == zoom) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
