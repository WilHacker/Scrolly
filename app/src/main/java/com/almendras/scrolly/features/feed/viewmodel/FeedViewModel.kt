package com.almendras.scrolly.features.feed.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.almendras.scrolly.features.feed.data.model.VideoItem
import com.almendras.scrolly.features.feed.data.repository.VideoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)

    private val _localVideos = MutableStateFlow<List<VideoItem>>(emptyList())

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val videos: StateFlow<List<VideoItem>> = combine(
        _localVideos,
        repository.getFavoriteIds()
    ) { localVideos, favoriteIds ->
        localVideos.map { video ->
            video.copy(isFavorite = favoriteIds.contains(video.id))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            // ACTIVAMOS EL SHIMMER
            _isLoading.value = true
            try {
                // Pequeño retraso para que el Shimmer sea visible y fluido
                delay(800)
                val foundVideos = repository.getLocalVideos()
                _localVideos.value = foundVideos
            } catch (_: Exception) {
                _localVideos.value = emptyList()
            } finally {
                // DESACTIVAMOS EL SHIMMER
                _isLoading.value = false
            }
        }
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video.id, video.isFavorite)
        }
    }
}