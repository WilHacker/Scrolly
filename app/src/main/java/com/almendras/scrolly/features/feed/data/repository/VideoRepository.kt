package com.almendras.scrolly.features.feed.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.almendras.scrolly.features.feed.data.db.AppDatabase
import com.almendras.scrolly.features.feed.data.db.FavoriteEntity
import com.almendras.scrolly.features.feed.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    private val videoDao = AppDatabase.getDatabase(context).videoDao()

    fun getFavoriteIds(): Flow<List<Long>> = videoDao.getAllFavoriteIds()

    // SOLUCIÓN: Envolvemos la llamada en Dispatchers.IO para que no bloquee la pantalla
    suspend fun toggleFavorite(videoId: Long, isCurrentlyFavorite: Boolean) = withContext(Dispatchers.IO) {
        val entity = FavoriteEntity(videoId = videoId)
        if (isCurrentlyFavorite) {
            videoDao.removeFavorite(entity)
        } else {
            videoDao.addFavorite(entity)
        }
    }

    suspend fun getLocalVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val folderColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Video Desconocido"
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val folderName = cursor.getString(folderColumn) ?: "Otros"

                val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                videos.add(
                    VideoItem(
                        id = id,
                        uri = contentUri,
                        name = name,
                        duration = duration,
                        size = size,
                        folderName = folderName,
                        isFavorite = false
                    )
                )
            }
        }
        videos
    }
}