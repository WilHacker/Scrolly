package com.almendras.scrolly.features.feed.domain.usecase

import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.repository.VideoRepository

class ToggleFavoriteUseCase(private val repository: VideoRepository) {
    suspend operator fun invoke(video: VideoItem) =
        repository.toggleFavorite(video.id, video.isFavorite)
}
