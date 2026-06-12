package com.almendras.scrolly.features.feed.data.repository

import com.almendras.scrolly.features.feed.data.local.db.FavoriteEntity
import com.almendras.scrolly.features.feed.data.local.db.VideoDao
import com.almendras.scrolly.features.feed.data.local.mediastore.MediaStoreVideoDataSource
import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

class VideoRepositoryImpl(
    private val mediaStoreDataSource: MediaStoreVideoDataSource,
    private val videoDao: VideoDao
) : VideoRepository {

    override suspend fun getLocalVideos(): List<VideoItem> =
        mediaStoreDataSource.queryVideos()

    override fun observeFavoriteIds(): Flow<List<Long>> =
        videoDao.getAllFavoriteIds()

    override suspend fun toggleFavorite(videoId: Long, isCurrentlyFavorite: Boolean) {
        val entity = FavoriteEntity(videoId = videoId)
        if (isCurrentlyFavorite) videoDao.removeFavorite(entity)
        else videoDao.addFavorite(entity)
    }
}
