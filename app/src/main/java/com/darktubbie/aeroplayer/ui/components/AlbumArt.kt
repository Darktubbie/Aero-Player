package com.darktubbie.aeroplayer.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.AlbumArtCache
import com.darktubbie.aeroplayer.R
import com.darktubbie.aeroplayer.ui.theme.AeroColors

/*
 * PORTADAS
 *
 * AlbumArtCache maneja todo el trabajo pesado (RAM -> disco ->
 * extracción -> marcador .none).
 *
 * Este Composable solamente solicita la portada cuando la
 * canción aparece en pantalla: carga lazy, no se procesan todas
 * las portadas durante el escaneo.
 *
 * Extraído de MainActivity sin cambiar comportamiento (Fase 4 del
 * plan).
 *
 * Fase 5 de la Experiencia de Artwork (0.5.0) — [animateChanges]:
 * por defecto (false) el comportamiento es idéntico al de siempre
 * (la portada se resetea a placeholder de inmediato al cambiar
 * [path]/[artist]/[album], como en Library/Home/Playlists/etc., sin
 * ningún costo de animación extra en listas con muchas filas).
 * Con `true` (usado en Now Playing y en el Mini Player, los dos
 * lugares donde el brief pidió transición), la portada ANTERIOR se
 * mantiene visible mientras se resuelve la nueva, y el cambio entre
 * una y otra (o hacia/desde el placeholder) se anima con
 * [Crossfade] en vez de reemplazarse de golpe.
 */
@Composable
fun AlbumArt(
    path: String?,
    artist: String,
    album: String,
    modifier: Modifier = Modifier,
    highRes: Boolean = false,
    animateChanges: Boolean = false
) {

    val context =
        LocalContext.current

    if (!animateChanges) {

        var bitmap by remember(
            path,
            artist,
            album,
            highRes
        ) {
            mutableStateOf<Bitmap?>(null)
        }

        LaunchedEffect(
            path,
            artist,
            album,
            highRes
        ) {

            bitmap =
                if (path.isNullOrBlank()) {
                    null
                } else {
                    AlbumArtCache.get(
                        context = context,
                        path = path,
                        artist = artist,
                        album = album,
                        highRes = highRes
                    )
                }
        }

        AlbumArtContent(
            bitmap = bitmap,
            modifier = modifier
        )

        return
    }

    /*
     * Variante animada: [bitmap] NO se resetea a null cuando
     * cambian las claves — se sobreescribe recién cuando la nueva
     * portada (o su ausencia) ya está resuelta, así que la anterior
     * queda en pantalla mientras tanto y [Crossfade] anima la
     * transición entre ambas.
     */
    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(
        path,
        artist,
        album,
        highRes
    ) {

        bitmap =
            if (path.isNullOrBlank()) {
                null
            } else {
                AlbumArtCache.get(
                    context = context,
                    path = path,
                    artist = artist,
                    album = album,
                    highRes = highRes
                )
            }
    }

    Crossfade(
        targetState = bitmap,
        animationSpec = tween(durationMillis = 350),
        label = "album-art-crossfade",
        modifier = modifier
    ) { crossfadeBitmap ->

        AlbumArtContent(
            bitmap = crossfadeBitmap,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AlbumArtContent(
    bitmap: Bitmap?,
    modifier: Modifier
) {

    if (
        bitmap != null
    ) {

        Image(

            bitmap =
                bitmap.asImageBitmap(),

            contentDescription =
                stringResource(R.string.cd_album_art),

            contentScale =
                ContentScale.Crop,

            modifier =
                modifier
        )

    } else {

        Box(

            modifier =
                modifier.background(
                    Brush.linearGradient(
                        colors =
                            AeroColors.AlbumArtPlaceholderGradient
                    )
                ),

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    "♪",

                color =
                    Color.White,

                fontSize =
                    24.sp
            )
        }
    }
}
