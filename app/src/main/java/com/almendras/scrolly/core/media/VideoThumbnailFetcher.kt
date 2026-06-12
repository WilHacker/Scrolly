package com.almendras.scrolly.core.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension

/**
 * Carga miniaturas de video usando las que Android ya tiene pre-generadas
 * (ContentResolver.loadThumbnail, API 29+). Es mucho más rápido que decodificar
 * un frame del archivo con MediaMetadataRetriever: el sistema las cachea en disco
 * y son las mismas que usa la galería nativa.
 *
 * En API < 29 devuelve null y Coil cae al VideoFrameDecoder normal.
 */
class VideoThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val width = (options.size.width as? Dimension.Pixels)?.px ?: DEFAULT_SIZE_PX
        val height = (options.size.height as? Dimension.Pixels)?.px ?: DEFAULT_SIZE_PX

        val bitmap = context.contentResolver.loadThumbnail(data, Size(width, height), null)

        return DrawableResult(
            drawable = bitmap.toDrawable(context.resources),
            isSampled = true,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            if (data.scheme != ContentResolver.SCHEME_CONTENT) return null
            if (data.authority != MediaStore.AUTHORITY) return null
            if (!data.pathSegments.contains("video")) return null
            return VideoThumbnailFetcher(context, data, options)
        }
    }

    private companion object {
        const val DEFAULT_SIZE_PX = 512
    }
}
