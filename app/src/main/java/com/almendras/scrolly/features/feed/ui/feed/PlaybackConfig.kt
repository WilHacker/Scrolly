package com.almendras.scrolly.features.feed.ui.feed

object PlaybackConfig {
    val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f, 2.5f, 3f, 3.5f, 4f)
    val ZOOM_OPTIONS = listOf(1f, 1.25f, 1.5f, 2f, 3f)

    const val DOUBLE_TAP_SEEK_MS = 10_000L
    const val HOLD_TO_SPEED_DELAY_MS = 400L
    const val HOLD_SPEED_FAST = 2.0f
    const val HOLD_SPEED_SLOW = 0.5f
    const val PROGRESS_UPDATE_MS = 250L
    const val SWIPE_PANEL_THRESHOLD_PX = 100f
}
