package com.almendras.scrolly.features.feed

import com.almendras.scrolly.features.feed.domain.model.VideoFilterType
import com.almendras.scrolly.features.feed.domain.model.applyFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoFilterTest {

    private val videos = listOf(
        testVideo(1, folder = "Camera"),
        testVideo(2, folder = "WhatsApp Video").copy(isFavorite = true),
        testVideo(3, folder = "Camera").copy(isFavorite = true)
    )

    @Test
    fun `ALL devuelve todos los videos`() {
        assertEquals(videos, videos.applyFilter(VideoFilterType.ALL, ""))
    }

    @Test
    fun `FAVORITES devuelve solo los favoritos`() {
        val result = videos.applyFilter(VideoFilterType.FAVORITES, "")
        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    @Test
    fun `FOLDER filtra por nombre exacto de carpeta`() {
        val result = videos.applyFilter(VideoFilterType.FOLDER, "Camera")
        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    @Test
    fun `FOLDER con carpeta inexistente devuelve vacio`() {
        assertEquals(emptyList<Any>(), videos.applyFilter(VideoFilterType.FOLDER, "NoExiste"))
    }

    @Test
    fun `from parsea el tipo y cae en ALL si es invalido`() {
        assertEquals(VideoFilterType.FAVORITES, VideoFilterType.from("FAVORITES"))
        assertEquals(VideoFilterType.ALL, VideoFilterType.from("cualquier_cosa"))
        assertEquals(VideoFilterType.ALL, VideoFilterType.from(null))
    }
}
