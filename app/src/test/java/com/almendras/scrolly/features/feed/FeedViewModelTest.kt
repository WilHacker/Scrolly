package com.almendras.scrolly.features.feed

import app.cash.turbine.test
import com.almendras.scrolly.MainDispatcherRule
import com.almendras.scrolly.features.feed.domain.usecase.GetVideosUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ObserveFavoriteIdsUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ToggleFavoriteUseCase
import com.almendras.scrolly.features.feed.ui.FeedViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeVideoRepository()

    private fun createViewModel() = FeedViewModel(
        getVideos = GetVideosUseCase(repository),
        observeFavoriteIds = ObserveFavoriteIdsUseCase(repository),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(repository)
    )

    @Test
    fun `loadVideos expone los videos del repositorio`() = runTest {
        repository.videos = listOf(testVideo(1), testVideo(2))
        val viewModel = createViewModel()

        viewModel.videos.test {
            assertEquals(emptyList<Any>(), awaitItem())

            viewModel.loadVideos()

            val loaded = awaitItem()
            assertEquals(listOf(1L, 2L), loaded.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `si el repositorio falla la lista queda vacia y no crashea`() = runTest {
        repository.videos = listOf(testVideo(1))
        repository.shouldFail = true
        val viewModel = createViewModel()

        viewModel.videos.test {
            assertEquals(emptyList<Any>(), awaitItem())

            viewModel.loadVideos()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `los favoritos se combinan con la lista de videos`() = runTest {
        repository.videos = listOf(testVideo(1), testVideo(2))
        val viewModel = createViewModel()

        viewModel.videos.test {
            awaitItem() // lista inicial vacía
            viewModel.loadVideos()
            val loaded = awaitItem()
            assertFalse(loaded.first { it.id == 1L }.isFavorite)

            viewModel.toggleFavorite(loaded.first { it.id == 1L })

            val updated = awaitItem()
            assertTrue(updated.first { it.id == 1L }.isFavorite)
            assertFalse(updated.first { it.id == 2L }.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quitar un favorito vuelve a isFavorite false`() = runTest {
        repository.videos = listOf(testVideo(1))
        val viewModel = createViewModel()

        viewModel.videos.test {
            awaitItem()
            viewModel.loadVideos()
            val loaded = awaitItem()

            viewModel.toggleFavorite(loaded.first())          // agregar
            val favorited = awaitItem()
            assertTrue(favorited.first().isFavorite)

            viewModel.toggleFavorite(favorited.first())       // quitar
            val unfavorited = awaitItem()
            assertFalse(unfavorited.first().isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
