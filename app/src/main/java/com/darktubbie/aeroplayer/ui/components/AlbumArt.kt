package com.darktubbie.aeroplayer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.sp
import com.darktubbie.aeroplayer.AlbumArtCache
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
 */
@Composable
fun AlbumArt(
    path: String?,
    artist: String,
    album: String,
    modifier: Modifier = Modifier
) {

    val context =
        LocalContext.current

    var bitmap by remember(
        path,
        artist,
        album
    ) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(
        path,
        artist,
        album
    ) {

        if (
            path.isNullOrBlank()
        ) {

            bitmap = null

            return@LaunchedEffect
        }

        bitmap =
            AlbumArtCache.get(
                context =
                    context,

                path =
                    path,

                artist =
                    artist,

                album =
                    album
            )
    }

    if (
        bitmap != null
    ) {

        Image(

            bitmap =
                bitmap!!.asImageBitmap(),

            contentDescription =
                "Album art",

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
