package com.almendras.scrolly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.almendras.scrolly.features.feed.ui.screens.FeedScreen
import com.almendras.scrolly.features.feed.ui.screens.GalleryScreen
import com.almendras.scrolly.features.feed.ui.screens.HomeScreen
import com.almendras.scrolly.features.feed.viewmodel.FeedViewModel
import com.almendras.scrolly.ui.theme.ScrollyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScrollyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    val navController = rememberNavController()

                    // El ViewModel se crea aquí para que sea el mismo en todas las pantallas
                    val feedViewModel: FeedViewModel = viewModel()

                    NavHost(navController = navController, startDestination = "home") {

                        // 1. Pantalla de Inicio (Dashboard)
                        composable("home") {
                            HomeScreen(
                                viewModel = feedViewModel,
                                onNavigateToGallery = { filterType, filterValue ->
                                    // Navega a la galería enviando el tipo de filtro y el valor
                                    navController.navigate("gallery/$filterType/$filterValue")
                                }
                            )
                        }

                        // 2. Galería Filtrada
                        composable(
                            route = "gallery/{filterType}/{filterValue}",
                            arguments = listOf(
                                navArgument("filterType") { type = NavType.StringType },
                                navArgument("filterValue") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val filterType = backStackEntry.arguments?.getString("filterType") ?: "ALL"
                            val filterValue = backStackEntry.arguments?.getString("filterValue") ?: ""

                            GalleryScreen(
                                viewModel = feedViewModel,
                                filterType = filterType,
                                filterValue = filterValue,
                                onVideoClick = { index ->
                                    navController.navigate("feed/$index")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 3. Reproductor (Feed)
                        composable(
                            route = "feed/{index}",
                            arguments = listOf(navArgument("index") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val index = backStackEntry.arguments?.getInt("index") ?: 0

                            FeedScreen(
                                viewModel = feedViewModel,
                                initialPage = index,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}