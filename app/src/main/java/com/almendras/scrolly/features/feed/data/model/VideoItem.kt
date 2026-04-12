package com.almendras.scrolly.features.feed.data.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val duration: Long,
    val size: Long,
    val folderName: String,         // NUEVO: Ej. "WhatsApp Video", "Camera"
    val isFavorite: Boolean = false // NUEVO: Para saber si tiene corazón
)