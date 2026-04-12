package com.almendras.scrolly.features.feed.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT videoId FROM favorites")
    fun getAllFavoriteIds(): Flow<List<Long>>

    // SOLUCIÓN: Quitamos 'suspend'. El repositorio se encargará del hilo de fondo.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addFavorite(favorite: FavoriteEntity)

    // SOLUCIÓN: Quitamos 'suspend'.
    @Delete
    fun removeFavorite(favorite: FavoriteEntity)
}