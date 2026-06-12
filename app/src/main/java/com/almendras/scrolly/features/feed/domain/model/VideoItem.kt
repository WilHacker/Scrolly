package com.almendras.scrolly.features.feed.domain.model

/**
 * Modelo de dominio puro (sin tipos de Android) para que sea testeable en JVM.
 * La uri se guarda como String; se convierte a Uri solo en la capa de UI/player.
 */
data class VideoItem(
    val id: Long,
    val uri: String,
    val name: String,
    val duration: Long,
    val size: Long,
    val folderName: String,
    val isFavorite: Boolean = false
)
