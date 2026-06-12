package com.almendras.scrolly.features.feed

import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeVideoRepository : VideoRepository {

    var videos: List<VideoItem> = emptyList()
    var shouldFail: Boolean = false

    private val favoriteIds = MutableStateFlow<List<Long>>(emptyList())

    override suspend fun getLocalVideos(): List<VideoItem> {
        if (shouldFail) throw RuntimeException("MediaStore no disponible")
        return videos
    }

    override fun observeFavoriteIds(): Flow<List<Long>> = favoriteIds

    override suspend fun toggleFavorite(videoId: Long, isCurrentlyFavorite: Boolean) {
        favoriteIds.update { current ->
            if (isCurrentlyFavorite) current - videoId else current + videoId
        }
    }
}

fun testVideo(id: Long, folder: String = "Camera") = VideoItem(
    id = id,
    uri = "content://media/external/video/media/$id",
    name = "video_$id.mp4",
    duration = 30_000,
    size = 1_000_000,
    folderName = folder
)
