package com.almendras.scrolly.features.feed.domain.usecase

import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.repository.VideoRepository

class GetVideosUseCase(private val repository: VideoRepository) {
    suspend operator fun invoke(): List<VideoItem> = repository.getLocalVideos()
}
