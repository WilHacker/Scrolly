package com.almendras.scrolly.features.feed.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.almendras.scrolly.R
import com.almendras.scrolly.core.ui.components.shimmerEffect
import com.almendras.scrolly.features.feed.domain.model.VideoFilterType
import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.model.applyFilter
import com.almendras.scrolly.features.feed.ui.FeedViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: FeedViewModel,
    filterType: VideoFilterType,
    filterValue: String,
    scrollToIndex: Int = -1,
    onVideoClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }

    val filteredVideos = remember(videos, filterType, filterValue) {
        videos.applyFilter(filterType, filterValue)
    }

    val gridState = rememberLazyGridState()

    // Al volver del feed, saltar al video donde quedaste
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex in filteredVideos.indices) {
            gridState.scrollToItem(scrollToIndex)
        }
    }

    val screenTitle = when (filterType) {
        VideoFilterType.FAVORITES -> stringResource(R.string.gallery_title_favorites)
        VideoFilterType.FOLDER -> filterValue
        VideoFilterType.ALL -> stringResource(R.string.gallery_title_all)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadVideos() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if (filteredVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.gallery_empty), color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(filteredVideos, key = { _, video -> video.id }) { index, video ->
                        VideoGridItem(video = video, imageLoader = imageLoader) {
                            onVideoClick(index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoGridItem(video: VideoItem, imageLoader: ImageLoader, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.DarkGray.copy(alpha = 0.4f))
            .clickable { onClick() }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(video.uri)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(modifier = Modifier.fillMaxSize().shimmerEffect())
            }
        )

        // Badge de duración (abajo a la derecha)
        Text(
            text = formatDuration(video.duration),
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        // Corazón si es favorito (arriba a la derecha)
        if (video.isFavorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFE91E63),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp)
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
