package com.almendras.scrolly.features.feed.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val videoId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
