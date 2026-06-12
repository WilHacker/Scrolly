package com.almendras.scrolly.core.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

/**
 * Puente entre el feed y el AudioService: el feed registra aquí el player de la
 * página activa para que la MediaSession del servicio controle exactamente lo
 * que el usuario está viendo/escuchando.
 */
object PlayerManager {

    @Volatile
    var activePlayer: ExoPlayer? = null

    @Volatile
    private var fallbackPlayer: ExoPlayer? = null

    /** Player para la MediaSession: el activo del feed, o uno de respaldo. */
    fun playerForSession(context: Context): ExoPlayer =
        activePlayer
            ?: fallbackPlayer
            ?: ExoPlayer.Builder(context.applicationContext).build().also { fallbackPlayer = it }

    fun releaseFallback() {
        fallbackPlayer?.release()
        fallbackPlayer = null
    }
}
