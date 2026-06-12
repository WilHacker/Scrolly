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
import com.almendras.scrolly.core.ui.theme.ScrollyTheme
import com.almendras.scrolly.features.feed.ui.FeedViewModel
import com.almendras.scrolly.navigation.ScrollyNavHost

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
                    val container = (application as ScrollyApp).container
                    // ViewModel compartido por las tres pantallas
                    val feedViewModel: FeedViewModel =
                        viewModel(factory = FeedViewModel.factory(container))

                    ScrollyNavHost(viewModel = feedViewModel)
                }
            }
        }
    }
}
