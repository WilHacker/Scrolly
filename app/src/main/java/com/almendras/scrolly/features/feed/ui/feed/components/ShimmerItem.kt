package com.almendras.scrolly.features.feed.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.almendras.scrolly.core.ui.components.shimmerEffect

/** Placeholder de página completa mientras se cargan los videos. */
@Composable
fun ShimmerItem() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Spacer(modifier = Modifier.fillMaxSize().shimmerEffect())
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .padding(bottom = 120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 20.dp)
                    .background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 15.dp)
                    .background(Color.Black.copy(0.4f), RoundedCornerShape(4.dp))
            )
        }
    }
}
