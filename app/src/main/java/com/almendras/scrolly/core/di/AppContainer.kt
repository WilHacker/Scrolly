package com.almendras.scrolly.core.di

import android.content.Context
import com.almendras.scrolly.features.feed.data.local.db.AppDatabase
import com.almendras.scrolly.features.feed.data.local.mediastore.MediaStoreVideoDataSource
import com.almendras.scrolly.features.feed.data.repository.VideoRepositoryImpl
import com.almendras.scrolly.features.feed.domain.repository.VideoRepository
import com.almendras.scrolly.features.feed.domain.usecase.GetVideosUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ObserveFavoriteIdsUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ToggleFavoriteUseCase

/**
 * Contenedor de dependencias manual (DI sin frameworks).
 * Vive en ScrollyApp y provee los use cases a los ViewModels vía factory.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    private val mediaStoreDataSource: MediaStoreVideoDataSource by lazy {
        MediaStoreVideoDataSource(appContext)
    }

    val videoRepository: VideoRepository by lazy {
        VideoRepositoryImpl(mediaStoreDataSource, database.videoDao())
    }

    val getVideos: GetVideosUseCase by lazy { GetVideosUseCase(videoRepository) }
    val observeFavoriteIds: ObserveFavoriteIdsUseCase by lazy { ObserveFavoriteIdsUseCase(videoRepository) }
    val toggleFavorite: ToggleFavoriteUseCase by lazy { ToggleFavoriteUseCase(videoRepository) }
}
