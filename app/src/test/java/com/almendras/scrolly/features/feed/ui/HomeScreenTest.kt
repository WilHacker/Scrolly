package com.almendras.scrolly.features.feed.ui

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.almendras.scrolly.core.ui.permissions.MediaPermissions
import com.almendras.scrolly.features.feed.FakeVideoRepository
import com.almendras.scrolly.features.feed.domain.model.VideoFilterType
import com.almendras.scrolly.features.feed.domain.usecase.GetVideosUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ObserveFavoriteIdsUseCase
import com.almendras.scrolly.features.feed.domain.usecase.ToggleFavoriteUseCase
import com.almendras.scrolly.features.feed.testVideo
import com.almendras.scrolly.features.feed.ui.home.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val repository = FakeVideoRepository()

    private fun createViewModel() = FeedViewModel(
        getVideos = GetVideosUseCase(repository),
        observeFavoriteIds = ObserveFavoriteIdsUseCase(repository),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(repository)
    )

    private fun grantVideoPermission() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(MediaPermissions.videoPermission)
    }

    @Test
    fun `con permiso muestra las carpetas de los videos`() {
        grantVideoPermission()
        repository.videos = listOf(
            testVideo(1, folder = "Camera"),
            testVideo(2, folder = "WhatsApp Video")
        )

        composeRule.setContent {
            HomeScreen(viewModel = createViewModel(), onNavigateToGallery = { _, _ -> })
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("WhatsApp Video").assertIsDisplayed()
    }

    @Test
    fun `tocar una carpeta navega con el filtro FOLDER`() {
        grantVideoPermission()
        repository.videos = listOf(testVideo(1, folder = "Camera"))

        var navigatedType: VideoFilterType? = null
        var navigatedValue: String? = null

        composeRule.setContent {
            HomeScreen(
                viewModel = createViewModel(),
                onNavigateToGallery = { type, value ->
                    navigatedType = type
                    navigatedValue = value
                }
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Camera").performClick()

        assertEquals(VideoFilterType.FOLDER, navigatedType)
        assertEquals("Camera", navigatedValue)
    }

    @Test
    fun `tocar Favoritos navega con el filtro FAVORITES`() {
        grantVideoPermission()
        repository.videos = listOf(testVideo(1))

        var navigatedType: VideoFilterType? = null

        composeRule.setContent {
            HomeScreen(
                viewModel = createViewModel(),
                onNavigateToGallery = { type, _ -> navigatedType = type }
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Favoritos").performClick()

        assertEquals(VideoFilterType.FAVORITES, navigatedType)
    }

    @Test
    fun `sin permiso muestra el boton de conceder permisos`() {
        composeRule.setContent {
            HomeScreen(viewModel = createViewModel(), onNavigateToGallery = { _, _ -> })
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conceder Permisos").assertIsDisplayed()
    }
}
