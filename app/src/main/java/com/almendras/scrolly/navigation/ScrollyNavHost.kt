package com.almendras.scrolly.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.almendras.scrolly.features.feed.domain.model.VideoFilterType
import com.almendras.scrolly.features.feed.ui.FeedViewModel
import com.almendras.scrolly.features.feed.ui.feed.FeedScreen
import com.almendras.scrolly.features.feed.ui.gallery.GalleryScreen
import com.almendras.scrolly.features.feed.ui.home.HomeScreen

private const val LAST_VIEWED_INDEX_KEY = "lastViewedIndex"

object Routes {
    const val HOME = "home"
    const val GALLERY = "gallery/{filterType}?filterValue={filterValue}"
    const val FEED = "feed/{filterType}/{index}?filterValue={filterValue}"

    fun gallery(filterType: VideoFilterType, filterValue: String = ""): String =
        "gallery/${filterType.name}?filterValue=${Uri.encode(filterValue)}"

    fun feed(filterType: VideoFilterType, index: Int, filterValue: String = ""): String =
        "feed/${filterType.name}/$index?filterValue=${Uri.encode(filterValue)}"
}

@Composable
fun ScrollyNavHost(viewModel: FeedViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToGallery = { filterType, filterValue ->
                    navController.navigate(Routes.gallery(filterType, filterValue))
                }
            )
        }

        composable(
            route = Routes.GALLERY,
            arguments = listOf(
                navArgument("filterType") { type = NavType.StringType },
                navArgument("filterValue") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val filterType = VideoFilterType.from(backStackEntry.arguments?.getString("filterType"))
            val filterValue = backStackEntry.arguments?.getString("filterValue").orEmpty()

            // El feed escribe aquí el último video visto para que la galería
            // haga scroll hasta él al volver
            val scrollToIndex by backStackEntry.savedStateHandle
                .getStateFlow(LAST_VIEWED_INDEX_KEY, -1)
                .collectAsState()

            GalleryScreen(
                viewModel = viewModel,
                filterType = filterType,
                filterValue = filterValue,
                scrollToIndex = scrollToIndex,
                onVideoClick = { index ->
                    navController.navigate(Routes.feed(filterType, index, filterValue))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FEED,
            arguments = listOf(
                navArgument("filterType") { type = NavType.StringType },
                navArgument("index") { type = NavType.IntType },
                navArgument("filterValue") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val filterType = VideoFilterType.from(backStackEntry.arguments?.getString("filterType"))
            val filterValue = backStackEntry.arguments?.getString("filterValue").orEmpty()
            val index = backStackEntry.arguments?.getInt("index") ?: 0

            FeedScreen(
                viewModel = viewModel,
                filterType = filterType,
                filterValue = filterValue,
                initialPage = index,
                onPageViewed = { viewedIndex ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(LAST_VIEWED_INDEX_KEY, viewedIndex)
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
