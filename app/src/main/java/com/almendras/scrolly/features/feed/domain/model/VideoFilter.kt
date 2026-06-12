package com.almendras.scrolly.features.feed.domain.model

enum class VideoFilterType {
    ALL, FAVORITES, FOLDER;

    companion object {
        fun from(value: String?): VideoFilterType =
            entries.firstOrNull { it.name == value } ?: ALL
    }
}

fun List<VideoItem>.applyFilter(type: VideoFilterType, value: String): List<VideoItem> =
    when (type) {
        VideoFilterType.ALL -> this
        VideoFilterType.FAVORITES -> filter { it.isFavorite }
        VideoFilterType.FOLDER -> filter { it.folderName == value }
    }
