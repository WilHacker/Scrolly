package com.almendras.scrolly.features.feed.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.almendras.scrolly.features.feed.data.local.db.AppDatabase
import com.almendras.scrolly.features.feed.data.local.db.FavoriteEntity
import com.almendras.scrolly.features.feed.data.local.db.VideoDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Test de integración del DAO con una base Room real en memoria. */
@RunWith(RobolectricTestRunner::class)
class VideoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: VideoDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.videoDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `agregar favorito y leerlo`() = runTest {
        dao.addFavorite(FavoriteEntity(videoId = 42))

        assertEquals(listOf(42L), dao.getAllFavoriteIds().first())
    }

    @Test
    fun `quitar favorito lo elimina`() = runTest {
        dao.addFavorite(FavoriteEntity(videoId = 1))
        dao.addFavorite(FavoriteEntity(videoId = 2))

        dao.removeFavorite(FavoriteEntity(videoId = 1))

        assertEquals(listOf(2L), dao.getAllFavoriteIds().first())
    }

    @Test
    fun `agregar el mismo favorito dos veces no duplica`() = runTest {
        dao.addFavorite(FavoriteEntity(videoId = 7))
        dao.addFavorite(FavoriteEntity(videoId = 7))

        assertEquals(listOf(7L), dao.getAllFavoriteIds().first())
    }

    @Test
    fun `el flow emite cuando cambian los favoritos`() = runTest {
        dao.getAllFavoriteIds().test {
            assertEquals(emptyList<Long>(), awaitItem())

            dao.addFavorite(FavoriteEntity(videoId = 5))
            assertEquals(listOf(5L), awaitItem())

            dao.removeFavorite(FavoriteEntity(videoId = 5))
            assertEquals(emptyList<Long>(), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
