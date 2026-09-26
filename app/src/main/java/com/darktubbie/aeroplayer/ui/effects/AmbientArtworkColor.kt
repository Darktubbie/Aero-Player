package com.darktubbie.aeroplayer.ui.effects

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.darktubbie.aeroplayer.AlbumArtCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Color dominante (promedio) de la portada de la canción actual,
 * para la integración de artwork con los fondos ambientales (Fase 5
 * de la Experiencia de Artwork, 0.5.0 — [MidgroundLayer] tiñe sus
 * dos degradados radiales con este color).
 *
 * Reutiliza [AlbumArtCache] tal cual (mismo bitmap pequeño que ya
 * usan las miniaturas de toda la app, normalmente ya en RAM cuando
 * la canción está sonando) en vez de decodificar nada por su
 * cuenta — esto NO agrega una segunda extracción de artwork, solo
 * promedia píxeles sobre el bitmap que [AlbumArtCache] ya resuelve.
 *
 * El promedio en sí se calcula sobre una copia reducida a 8x8 (64
 * píxeles): suficiente para un color de ambiente y trivial en CPU,
 * hecho una sola vez por álbum y cacheado en RAM aquí (no hace falta
 * volver a promediar cada vez que cambia de canción dentro del mismo
 * álbum).
 */
object AmbientArtworkColor {

    private const val SAMPLE_SIZE = 8

    private val colorCache =
        android.util.LruCache<String, Color>(24)

    /**
     * @return el color dominante, o null si la canción no tiene
     * artwork embebido (mismo criterio que [AlbumArtCache]: sin
     * portada, sin color — [MidgroundLayer] cae de vuelta al color
     * fijo del tema en ese caso).
     */
    suspend fun get(
        context: Context,
        path: String,
        artist: String,
        album: String
    ): Color? {

        if (path.isBlank()) {
            return null
        }

        val cacheKey =
            "${artist.trim().lowercase()}|${album.trim().lowercase()}|$path"

        colorCache.get(cacheKey)?.let {
            return it
        }

        val bitmap =
            AlbumArtCache.get(
                context = context,
                path = path,
                artist = artist,
                album = album,
                highRes = false
            ) ?: return null

        val color =
            withContext(Dispatchers.Default) {
                averageColor(bitmap)
            }

        colorCache.put(
            cacheKey,
            color
        )

        return color
    }

    private fun averageColor(
        bitmap: Bitmap
    ): Color {

        val scaled =
            Bitmap.createScaledBitmap(
                bitmap,
                SAMPLE_SIZE,
                SAMPLE_SIZE,
                true
            )

        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L

        val pixelCount =
            SAMPLE_SIZE * SAMPLE_SIZE

        val pixels =
            IntArray(pixelCount)

        scaled.getPixels(
            pixels,
            0,
            SAMPLE_SIZE,
            0,
            0,
            SAMPLE_SIZE,
            SAMPLE_SIZE
        )

        if (scaled !== bitmap) {
            scaled.recycle()
        }

        for (pixel in pixels) {

            totalRed += (pixel shr 16) and 0xFF
            totalGreen += (pixel shr 8) and 0xFF
            totalBlue += pixel and 0xFF
        }

        return Color(
            red = (totalRed / pixelCount) / 255f,
            green = (totalGreen / pixelCount) / 255f,
            blue = (totalBlue / pixelCount) / 255f,
            alpha = 1f
        )
    }
}
