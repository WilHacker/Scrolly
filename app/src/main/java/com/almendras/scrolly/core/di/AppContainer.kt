package com.almendras.scrolly.core.di

import android.content.Context
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import com.almendras.scrolly.core.media.VideoThumbnailFetcher
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

    /**
     * Singleton: el caché de memoria sobrevive a la navegación entre pantallas
     * (antes se creaba un loader nuevo por pantalla y re-decodificaba todo).
     * Prioriza las miniaturas pre-generadas del sistema; si no, decodifica un frame.
     */
    val imageLoader: ImageLoader by lazy {
        ImageLoader.Builder(appContext)
            .components {
                add(VideoThumbnailFetcher.Factory(appContext))
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .crossfade(true)
            .build()
    }

    val getVideos: GetVideosUseCase by lazy { GetVideosUseCase(videoRepository) }
    val observeFavoriteIds: ObserveFavoriteIdsUseCase by lazy { ObserveFavoriteIdsUseCase(videoRepository) }
    val toggleFavorite: ToggleFavoriteUseCase by lazy { ToggleFavoriteUseCase(videoRepository) }
}
