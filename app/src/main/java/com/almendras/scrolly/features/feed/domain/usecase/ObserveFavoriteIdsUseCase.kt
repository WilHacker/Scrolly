package com.almendras.scrolly.features.feed.domain.usecase

import com.almendras.scrolly.features.feed.domain.repository.VideoRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoriteIdsUseCase(private val repository: VideoRepository) {
    operator fun invoke(): Flow<List<Long>> = repository.observeFavoriteIds()
}
