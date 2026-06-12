package com.almendras.scrolly.core.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.abs

/**
 * Pool de ExoPlayers reutilizables para el feed vertical (estilo TikTok/Reels).
 *
 * En vez de crear y destruir un ExoPlayer por página (inicializar códecs cuesta
 * 100-300 ms y causa el tirón al deslizar), se mantienen pocas instancias vivas
 * que se reciclan entre páginas: la actual, la anterior y la siguiente quedan
 * preparadas, así el swipe encuentra el primer frame ya decodificado.
 */
@UnstableApi
class PlayerPool(context: Context, private val capacity: Int = 6) {

    private val appContext = context.applicationContext

    // página -> player asignado a esa página
    private val byPage = mutableMapOf<Int, ExoPlayer>()
    private val free = ArrayDeque<ExoPlayer>()

    fun acquire(page: Int): ExoPlayer {
        byPage[page]?.let { return it }
        val player = free.removeFirstOrNull()
            ?: if (byPage.size + free.size < capacity) createPlayer() else stealFarthestFrom(page)
        byPage[page] = player
        return player
    }

    /** Devuelve el player de la página al pool sin destruir los códecs. */
    fun recycle(page: Int) {
        val player = byPage.remove(page) ?: return
        player.stop()
        player.clearMediaItems()
        free.addLast(player)
    }

    fun playerFor(page: Int): ExoPlayer? = byPage[page]

    fun releaseAll() {
        byPage.values.forEach { it.release() }
        free.forEach { it.release() }
        byPage.clear()
        free.clear()
    }

    private fun stealFarthestFrom(page: Int): ExoPlayer {
        val victim = byPage.keys.maxBy { abs(it - page) }
        return byPage.remove(victim)!!.apply {
            stop()
            clearMediaItems()
        }
    }

    private fun createPlayer(): ExoPlayer {
        // Buffers cortos: arranque casi instantáneo y menos memoria con varios players
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 5_000,
                /* maxBufferMs = */ 20_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .build()

        // Decoders FFmpeg (Jellyfin) como respaldo: si el hardware del dispositivo
        // no soporta el códec (AC3, DTS, Vorbis...), decodifica por software
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        return ExoPlayer.Builder(appContext, renderersFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = false
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus = */ true
                )
                setHandleAudioBecomingNoisy(true)
            }
    }
}
