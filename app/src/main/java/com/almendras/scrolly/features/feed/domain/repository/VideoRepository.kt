package com.almendras.scrolly.features.feed.domain.repository

import com.almendras.scrolly.features.feed.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow

interface VideoRepository {
    suspend fun getLocalVideos(): List<VideoItem>
    fun observeFavoriteIds(): Flow<List<Long>>
    suspend fun toggleFavorite(videoId: Long, isCurrentlyFavorite: Boolean)
}
