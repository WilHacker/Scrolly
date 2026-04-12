package com.almendras.scrolly.features.feed.service

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {
    private var exoPlayer: ExoPlayer? = null

    fun getPlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                repeatMode = ExoPlayer.REPEAT_MODE_ONE
            }
        }
        return exoPlayer!!
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }
}