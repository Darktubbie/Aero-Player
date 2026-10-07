package com.darktubbie.aeroplayer.ui.effects

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.darktubbie.aeroplayer.AlbumArtCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Paleta ambiental de la portada actual (Fase 9, 0.6.0 — Album Art
 * as Environment): dos colores para iluminar el entorno en vez de
 * uno solo.
 *
 * [primary] es el color más "vivo" y representativo de la portada;
 * [secondary] es un segundo color de otro tono (o, si la portada es
 * casi monocromática, un tono vecino del primario). Ambos ya vienen
 * ajustados a un rango de saturación/brillo que se ve bien como luz
 * de ambiente sobre los dos temas.
 */
data class ArtworkPalette(
    val primary: Color,
    val secondary: Color
)

/**
 * Colores de la portada de la canción actual para la integración de
 * artwork con los fondos ambientales ([MidgroundLayer]).
 *
 * Fase 5 de la Experiencia de Artwork (0.5.0): un promedio simple
 * de la portada. Fase 9 (0.6.0): el promedio se queda como respaldo
 * (portadas casi grises), pero el color principal ahora sale de un
 * histograma de tonos ponderado por saturación, porque el promedio
 * puro de una portada colorida suele dar un marrón/gris apagado que
 * no se parece a la portada.
 *
 * Reutiliza [AlbumArtCache] tal cual (mismo bitmap pequeño que ya
 * usan las miniaturas de toda la app, normalmente ya en RAM cuando
 * la canción está sonando) — NO agrega una segunda extracción de
 * artwork. El análisis corre sobre una copia reducida a 16x16 (256
 * píxeles), una sola vez por álbum, y se cachea en RAM aquí.
 */
object AmbientArtworkColor {

    private const val SAMPLE_SIZE = 16

    private const val HUE_BUCKETS = 12

    private val paletteCache =
        android.util.LruCache<String, ArtworkPalette>(24)

    /**
     * Color principal (compatibilidad con la Fase 5).
     *
     * @return null si la canción no tiene artwork embebido.
     */
    suspend fun get(
        context: Context,
        path: String,
        artist: String,
        album: String
    ): Color? =
        getPalette(context, path, artist, album)?.primary

    /**
     * @return la paleta, o null si la canción no tiene artwork
     * embebido (mismo criterio que [AlbumArtCache]: sin portada, sin
     * color — [MidgroundLayer] cae de vuelta al color fijo del tema).
     */
    suspend fun getPalette(
        context: Context,
        path: String,
        artist: String,
        album: String
    ): ArtworkPalette? {

        if (path.isBlank()) {
            return null
        }

        val cacheKey =
            "${artist.trim().lowercase()}|${album.trim().lowercase()}|$path"

        paletteCache.get(cacheKey)?.let {
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

        val palette =
            withContext(Dispatchers.Default) {
                extract(bitmap)
            }

        paletteCache.put(
            cacheKey,
            palette
        )

        return palette
    }

    private fun extract(
        bitmap: Bitmap
    ): ArtworkPalette {

        val scaled =
            Bitmap.createScaledBitmap(
                bitmap,
                SAMPLE_SIZE,
                SAMPLE_SIZE,
                true
            )

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

        var totalRed = 0L
        var totalGreen = 0L
        var totalBlue = 0L

        val weight = FloatArray(HUE_BUCKETS)
        val sumR = FloatArray(HUE_BUCKETS)
        val sumG = FloatArray(HUE_BUCKETS)
        val sumB = FloatArray(HUE_BUCKETS)

        var totalWeight = 0f

        val hsv = FloatArray(3)

        for (pixel in pixels) {

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            totalRed += r
            totalGreen += g
            totalBlue += b

            android.graphics.Color.RGBToHSV(r, g, b, hsv)

            // Los píxeles casi grises/negros/blancos casi no
            // cuentan: no tienen "tono" que aportar a la luz.
            val w =
                hsv[1] * (0.35f + 0.65f * hsv[2])

            val bucket =
                (hsv[0] / (360f / HUE_BUCKETS))
                    .toInt()
                    .coerceIn(0, HUE_BUCKETS - 1)

            weight[bucket] += w
            sumR[bucket] += r * w
            sumG[bucket] += g * w
            sumB[bucket] += b * w

            totalWeight += w
        }

        val average =
            Color(
                red = (totalRed / pixelCount) / 255f,
                green = (totalGreen / pixelCount) / 255f,
                blue = (totalBlue / pixelCount) / 255f,
                alpha = 1f
            )

        // Portada casi monocromática (grises/blanco y negro): no hay
        // un tono que destacar, se usa el promedio para ambos.
        if (totalWeight < pixelCount * 0.06f) {

            return ArtworkPalette(
                primary = average,
                secondary = average
            )
        }

        var best = 0

        for (i in 1 until HUE_BUCKETS) {
            if (weight[i] > weight[best]) {
                best = i
            }
        }

        val primary =
            bucketColor(sumR, sumG, sumB, weight, best)

        // Segundo color: el mejor tono que esté al menos a 2
        // cubetas (60°) del principal y pese algo razonable.
        var second = -1

        for (i in 0 until HUE_BUCKETS) {

            val distance =
                minOf(
                    (i - best + HUE_BUCKETS) % HUE_BUCKETS,
                    (best - i + HUE_BUCKETS) % HUE_BUCKETS
                )

            if (
                distance >= 2 &&
                weight[i] >= weight[best] * 0.25f &&
                (second == -1 || weight[i] > weight[second])
            ) {
                second = i
            }
        }

        val secondary =
            if (second != -1) {

                bucketColor(sumR, sumG, sumB, weight, second)

            } else {

                // Sin segundo tono: uno vecino (+35°) del principal.
                shiftHue(primary, 35f)
            }

        return ArtworkPalette(
            primary = vivid(primary),
            secondary = vivid(secondary)
        )
    }

    private fun bucketColor(
        sumR: FloatArray,
        sumG: FloatArray,
        sumB: FloatArray,
        weight: FloatArray,
        index: Int
    ): Color {

        val w =
            weight[index].coerceAtLeast(0.0001f)

        return Color(
            red = (sumR[index] / w) / 255f,
            green = (sumG[index] / w) / 255f,
            blue = (sumB[index] / w) / 255f,
            alpha = 1f
        )
    }

    private fun shiftHue(
        color: Color,
        degrees: Float
    ): Color {

        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            color.toArgbOpaque(),
            hsv
        )

        hsv[0] = (hsv[0] + degrees) % 360f

        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    /**
     * Lleva el color a un rango que funcione como luz de ambiente:
     * lo bastante saturado para leerse como color y ni tan oscuro
     * que desaparezca ni tan claro que lave el fondo.
     */
    private fun vivid(
        color: Color
    ): Color {

        val hsv = FloatArray(3)

        android.graphics.Color.colorToHSV(
            color.toArgbOpaque(),
            hsv
        )

        hsv[1] = hsv[1].coerceIn(0.35f, 0.90f)
        hsv[2] = hsv[2].coerceIn(0.50f, 0.95f)

        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun Color.toArgbOpaque(): Int =
        android.graphics.Color.rgb(
            (red * 255f).toInt().coerceIn(0, 255),
            (green * 255f).toInt().coerceIn(0, 255),
            (blue * 255f).toInt().coerceIn(0, 255)
        )
}
