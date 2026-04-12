package com.almendras.scrolly.features.feed.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: Long, // Guardamos el ID único del video
    val addedAt: Long = System.currentTimeMillis() // Fecha exacta en la que le diste like
)