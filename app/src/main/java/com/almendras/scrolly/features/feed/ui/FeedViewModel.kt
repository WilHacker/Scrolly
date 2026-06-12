package com.almendras.scrolly.features.feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.almendras.scrolly.core.di.AppContainer
import com.almendras.scrolly.features.feed.domain.model.VideoItem
import com.almendras.scrolly.features.feed.domain.usecase.GetVideosUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ObserveFavoriteIdsUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(
    private val getVideos: GetVideosUseCase,
    observeFavoriteIds: ObserveFavoriteIdsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase
) : ViewModel() {

    private val _localVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val videos: StateFlow<List<VideoItem>> = combine(
        _localVideos,
        observeFavoriteIds()
    ) { localVideos, favoriteIds ->
        localVideos.map { video ->
            video.copy(isFavorite = video.id in favoriteIds)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Se llama cuando el permiso de lectura de videos está concedido. */
    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _localVideos.value = getVideos()
            } catch (_: Exception) {
                _localVideos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            toggleFavoriteUseCase(video)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                FeedViewModel(
                    getVideos = container.getVideos,
                    observeFavoriteIds = container.observeFavoriteIds,
                    toggleFavoriteUseCase = container.toggleFavorite
                )
            }
        }
    }
}
